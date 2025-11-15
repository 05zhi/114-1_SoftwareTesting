package org.example;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class PlayByPlayAnnouncer {

    // ========= Logger =========
    private static final Logger logger = LogManager.getLogger(PlayByPlayAnnouncer.class);

    // ========= 自訂例外：速記錯誤 =========
    public static class ScoreNotationException extends Exception {
        public ScoreNotationException(String message) {
            super(message);
        }
    }

    // ========= 單局統計結果（給 JUnit 用） =========
    public static class InningSummary {
        public final int runs;       // 得分 R
        public final int hits;       // 安打 H
        public final int outs;       // 出局 O
        public final int lob;        // 殘壘數
        public final String baseState; // 壘上描述

        public InningSummary(int runs, int hits, int outs, int lob, String baseState) {
            this.runs = runs;
            this.hits = hits;
            this.outs = outs;
            this.lob = lob;
            this.baseState = baseState;
        }
    }

    // ========= 整場比賽用的狀態 =========
    public static class TeamState {
        public final String name;
        public final String[] lineup;   // 1~9 棒打序
        public int batterIndex = 0;     // 下一棒打者 index (0~8)
        public final int[] inningRuns = new int[20]; // 每局得分（預留延長）
        public int totalRuns = 0;

        public TeamState(String name, String[] lineup) {
            this.name = name;
            this.lineup = lineup;
        }
    }

    // 用來回傳「單一半局」的結果 + 下一棒打者
    private static class HalfInningResult {
        public final InningSummary summary;
        public final int nextBatterIndex;

        public HalfInningResult(InningSummary summary, int nextBatterIndex) {
            this.summary = summary;
            this.nextBatterIndex = nextBatterIndex;
        }
    }

    // --- 球員名單設定 ---
    private static final String[] DODGERS_BATTERS_LIST = {
            "Shohei Ohtani", "Mookie Betts", "Freddie Freeman",
            "Will Smith", "Max Muncy", "Teo Hernandez",
            "Tommy Edman", "Andy Pages", "Miguel Rojas"
    };

    // 藍鳥隊 (守備方) - 守備位置及球員名單
    private static final String[] BLUE_JAYS_BATTERS_LIST = {
            "George Springer",       // 1
            "Bo Bichette",           // 2
            "Vladimir Guerrero Jr.", // 3
            "Daulton Varsho",        // 4
            "Justin Turner",         // 5
            "Davis Schneider",       // 6
            "Alejandro Kirk",        // 7
            "Isiah Kiner-Falefa",    // 8
            "Ernie Clement"          // 9
    };

    // Test Case 1: 正常流程、得分與殘壘
    private static final String[] t01 = {
            "BB",         // 保送
            "1B",         // 一壘安打
            "K",          // 三振 (1 out)
            "2B 1R",      // 二壘安打，1分得分
            "F8",         // 中外野高飛接殺 (2 out)
            "6-3",        // 游擊傳一壘，滾地球出局 (3 out)
    };

    // Test Case 2: 3 Out 後的速記錯誤
    private static final String[] t02 = {
            "K",          // 三振 (1 out)
            "K",          // 三振 (2 out)
            "K",          // 三振 (3 out) -> 局數結束
            "1B",         // 【3 Out 後的無效事件】
    };

    // Test Case 3: 包含無法解析的速記代碼
    private static final String[] t03 = {
            "BB",         // 保送
            "K",          // 三振
            "3B 1R",      // 三壘安打
            "WTF",        // 【無法解析的速記錯誤】
            "HR 3R",      // 全壘打
    };

    // Test Case 4: 保送後安打
    private static final String[] t04 = {
            "BB",         // 保送
            "BB",         // 保送
            "BB",         // 保送
            "BB 1R",      // 保送
            "2B 2R",      // 二壘安打, 一三壘有人
            "K",
            "BB",         // 保送, 滿壘
            "K",
    };


    // 從 "2B 1R" 抓出 1
    public static int extractRuns(String event) {
        Matcher m = Pattern.compile("(\\d+)R").matcher(event);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }

    // 壘上狀態
    private static String baseStateDesc(boolean[] bases) {
        List<String> desc = new ArrayList<>();
        if (bases[2]) desc.add("三壘");
        if (bases[1]) desc.add("二壘");
        if (bases[0]) desc.add("一壘");
        return desc.isEmpty() ? "無人在壘" : String.join("、", desc) + "有人";
    }

    // 殘壘
    private static int calculateLOB(boolean[] bases) {
        int c = 0;
        for (boolean b : bases) if (b) c++;
        return c;
    }

    // 壘上目前有幾個跑者
    private static int countRunnersOnBase(boolean[] bases) {
        int c = 0;
        for (boolean b : bases) if (b) c++;
        return c;
    }

    /**
     * ✅ 進壘與得分（一般情況）
     *  - baseAdvance = 1 → 1B / BB / E
     *  - baseAdvance = 2 → 2B
     *  - baseAdvance = 3 → 3B
     *  - baseAdvance = 4 → HR
     *
     * @param bases       目前壘包狀態（index: 0=一壘,1=二壘,2=三壘）
     * @param baseAdvance 要前進幾個壘包
     * @param isBBorE     是否為 BB / E（打者固定上一壘）
     * @return 此事件在「自動跑壘」下產生的得分（不含 Rn）
     */
    private static int advanceRunners(boolean[] bases, int baseAdvance, boolean isBBorE) {
        int autoRuns = 0;
        boolean[] next = new boolean[3];

        // 先處理原本壘上的跑者
        for (int i = 2; i >= 0; i--) {
            if (!bases[i]) continue;
            int originBase = i + 1;
            int newBase = originBase + baseAdvance;
            if (newBase > 3) {
                autoRuns++;
            } else {
                next[newBase - 1] = true;
            }
        }

        // 打者的去處
        int batterDest;
        if (baseAdvance == 4) {
            batterDest = 4; // 全壘打
        } else if (isBBorE) {
            batterDest = 1; // 保送 / 失誤 → 上一壘
        } else {
            batterDest = baseAdvance;   // 1B / 2B / 3B
        }

        if (batterDest > 3) {
            autoRuns++;
        } else if (batterDest >= 1) {
            next[batterDest - 1] = true;
        }

        // 更新壘包
        System.arraycopy(next, 0, bases, 0, 3);
        return autoRuns;
    }

    private static int outsFromGroundPlay(String event) {
        String firstToken = event.split(" ")[0];
        String digitsOnly = firstToken.replaceAll("[^0-9]", "");
        int len = digitsOnly.length();
        return Math.max(1, len - 1);
    }

    // ========= 單一半局模擬 =========

    private static HalfInningResult simulateHalfInning(String[] lineup,
                                                       int startingBatterIndex,
                                                       String[] events,
                                                       boolean logExtraAfter3Out) {
        boolean[] bases = new boolean[]{false, false, false};
        int runs = 0;
        int hits = 0;
        int outs = 0;
        int batterIndex = startingBatterIndex;

        try {
            for (String event : events) {

                // 已經 3 out 又有事件
                if (outs == 3) {
                    if (logExtraAfter3Out) {
                        logger.error("3 Out 之後仍出現速記事件：" + event);
                    }
                    break;
                }

                String batter = lineup[batterIndex % lineup.length];

                int explicitRuns = extractRuns(event);
                boolean isBB = event.startsWith("BB");
                boolean isE = event.startsWith("E");
                boolean isHitLike =
                        event.startsWith("1B") ||
                                event.startsWith("2B") ||
                                event.startsWith("3B") ||
                                event.startsWith("HR");
                boolean advance = isBB || isE || isHitLike;

                if (advance) {
                    int baseAdvance = 0;
                    boolean countAsHit = false;

                    if (isBB || isE) {
                        baseAdvance = 1; // BB、E 都視為前進一壘
                    } else if (event.startsWith("1B")) {
                        baseAdvance = 1;
                        countAsHit = true;
                    } else if (event.startsWith("2B")) {
                        baseAdvance = 2;
                        countAsHit = true;
                    } else if (event.startsWith("3B")) {
                        baseAdvance = 3;
                        countAsHit = true;
                    } else if (event.startsWith("HR")) {
                        baseAdvance = 4;
                        countAsHit = true;
                    }

                    if (countAsHit) {
                        hits++;
                    }

                    // 檢查 Rn 是否合理
                    int runnersBefore = countRunnersOnBase(bases); // 事件發生前壘上人數
                    if (explicitRuns > 0) {
                        int maxPossibleRuns = runnersBefore + 1;   // 壘上所有跑者 + 打者
                        if (explicitRuns > maxPossibleRuns) {
                            String msg = "異常速記：壘上僅有 " + runnersBefore +
                                    " 人，事件 '" + event + "' 卻標示 " + explicitRuns + "R";
                            logger.error(msg);
                            throw new ScoreNotationException(msg);
                        }
                    }

                    int autoRuns = advanceRunners(bases, baseAdvance, isBB || isE);

                    // Rn
                    int addRuns;
                    if (explicitRuns > 0) {
                        // 有標示 Rn
                        addRuns = explicitRuns;
                    } else {
                        // 沒標示 Rn
                        addRuns = autoRuns;
                    }
                    runs += addRuns;

                    batterIndex++;

                } else {
                    if (event.equals("K")) {
                        outs = Math.min(3, outs + 1);
                        batterIndex++;
                    } else if (event.startsWith("F")) {
                        outs = Math.min(3, outs + 1);
                        batterIndex++;
                    } else if (event.matches("^[0-9].*?-.*")) {
                        int addOuts = outsFromGroundPlay(event);
                        outs = Math.min(3, outs + addOuts);
                        batterIndex++;
                    } else {
                        throw new ScoreNotationException(
                                "無法解析速記代碼: " + event + ", 打者: " + batter);
                    }
                }
            }
        } catch (ScoreNotationException e) {
            logger.error(e.getMessage());
        }

        int lob = calculateLOB(bases);
        String baseText = baseStateDesc(bases);

        InningSummary summary = new InningSummary(runs, hits, outs, lob, baseText);
        int nextIdx = batterIndex % lineup.length;
        return new HalfInningResult(summary, nextIdx);
    }

    public static InningSummary getInningSummary(String[] events) {
        HalfInningResult result =
                simulateHalfInning(DODGERS_BATTERS_LIST, 0, events, true);
        return result.summary;
    }

    // 單局輸出
    public static void show(InningSummary s) {
        System.out.println("R=" + s.runs);
        System.out.println("H=" + s.hits);
        System.out.println("O=" + s.outs);
        System.out.println("LOB=" + s.lob + " (" + s.baseState + ")");
    }

    public static void announceInning(String[] events) {
        System.out.println("====== MLB 播報 ======");
        InningSummary summary = getInningSummary(events);
        show(summary);
    }

    // ========= 整場比賽：上下半局 + 九局 + 打序延續 =========

    /**
     * topEventsByInning[i] : 第 i+1 局上（客隊）速記陣列
     * botEventsByInning[i] : 第 i+1 局下（主隊）速記陣列
     */
    public static void simulateGame(String[][] topEventsByInning,
                                    String[][] botEventsByInning) {

        TeamState away = new TeamState("Dodgers", DODGERS_BATTERS_LIST);
        TeamState home = new TeamState("Blue Jays", BLUE_JAYS_BATTERS_LIST);

        int innings = Math.max(
                topEventsByInning == null ? 0 : topEventsByInning.length,
                botEventsByInning == null ? 0 : botEventsByInning.length
        );

        System.out.println("\n========== 比賽開始 ==========\n");

        for (int i = 0; i < innings; i++) {
            int inningNo = i + 1;

            // ----- 局上：客隊 -----
            System.out.println("----- 第 " + inningNo + " 局上：客隊 " + away.name + " 進攻 -----");
            if (topEventsByInning != null && i < topEventsByInning.length
                    && topEventsByInning[i] != null) {
                HalfInningResult r = simulateHalfInning(
                        away.lineup, away.batterIndex, topEventsByInning[i], true);
                away.batterIndex = r.nextBatterIndex;
                away.inningRuns[i] = r.summary.runs;
                away.totalRuns += r.summary.runs;
                show(r.summary);
            } else {
                System.out.println("(本局無速記事件)");
            }

            // ----- 局下：主隊 -----
            System.out.println("----- 第 " + inningNo + " 局下：主隊 " + home.name + " 進攻 -----");
            if (botEventsByInning != null && i < botEventsByInning.length
                    && botEventsByInning[i] != null) {
                HalfInningResult r = simulateHalfInning(
                        home.lineup, home.batterIndex, botEventsByInning[i], true);
                home.batterIndex = r.nextBatterIndex;
                home.inningRuns[i] = r.summary.runs;
                home.totalRuns += r.summary.runs;
                show(r.summary);
            } else {
                System.out.println("(本局無速記事件)");
            }

            System.out.println();
        }

        System.out.println("========== 記分板 ==========");
        System.out.print("Inning : ");
        for (int i = 0; i < innings; i++) {
            System.out.print((i + 1) + " ");
        }
        System.out.println("| R");

        System.out.print(away.name + " : ");
        for (int i = 0; i < innings; i++) {
            System.out.print(away.inningRuns[i] + " ");
        }
        System.out.println("| " + away.totalRuns);

        System.out.print(home.name + " : ");
        for (int i = 0; i < innings; i++) {
            System.out.print(home.inningRuns[i] + " ");
        }
        System.out.println("| " + home.totalRuns);
        System.out.println("========== 比賽結束 ==========\n");
    }

    public static void main(String[] args) {

        announceInning(t01);
        announceInning(t02);
        announceInning(t03);
        announceInning(t04);

        String[][] top = {
                t01,  // 1 局上
                t02,  // 2 局上
                t04   // 3 局上
        };

        String[][] bot = {
                t02,  // 1 局下
                t03,  // 2 局下
                t01   // 3 局下
        };

        simulateGame(top, bot);
    }
}
