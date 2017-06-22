/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017  Diversity Arrays Technology, Pty Ltd.

    KDXplore may be redistributed and may be modified under the terms
    of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option)
    any later version.

    KDXplore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.

    You should have received a copy of the GNU General Public License
    along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.diversityarrays.util;

/**
 * Potentially useful Unicode characters (symbols)
 *
 * All summarised at:
 * http://en.wikipedia.org/wiki/Unicode_block
 *
 * Possibly useful are:
 *
 * http://en.wikipedia.org/wiki/Supplemental_Arrows-A
 * http://en.wikipedia.org/wiki/Supplemental_Arrows-B
 * http://en.wikipedia.org/wiki/Miscellaneous_Symbols_and_Arrows
 *
 * U+26A0	 Warning sign,
 * 26A1 lightning/high voltage
 *
 * Crossing:  26a2 f/f
 *            26a3 m/m
 *            26a4 m/f ⚤
 *            26a5  hermaphrodite fem-S/male-NE
 *            26a6 trans  (fem/male)-NE
 *
 * 2620 ☠ Skull
 * 222E ∮ Integral circled
 *
 * 233B ⌻ small circle in square
 * 233C ⌼ large circle in square
 *
 * Math symbols B
 * 29C7 ⧇ circle in square
 * 29C8 ⧈ square in square
 *
 *
 * 2795 ➕ heavy plus
 * FF0B ＋ "full width plus sign"
 *
 * 274F, 2750, 2751, 2752   DOES NOT SHOW ON ANDROID
 * ❏    ❐     ❑     ❒
 * LR,   UR,   LR,   UR   shadowed white squat\re
 *
 * 29FA ⧺ double-plus
 * 2707 ✇ Tape drive
 * 2318 ⌘ Place of interest  DOES NOT SHOW ON ANDROID
 *
 * Die Faces
 * 2680 ⚀ 1
 * 2681 ⚁ 2
 * 2682 ⚂ 3
 * 2683 ⚃ 4   DOES SHOW
 * 2684 ⚄ 5
 * 2685 ⚅ 6
 *
 * 25A2 ▢
 * 25A6 ▦  Square with orthogonal crosshatch
 *            sb.append(String.format("\n\u25a6:%.0f%%\n", scoredFraction * 100f));
 *
 *
 * http://en.wikipedia.org/wiki/Block_Elements
 * ===========================================
 *
 * 2591 ░ Light shade
 * 2592 ▒ Medium shade
 * 2593 ▓ Heavy shade
 *
 *
 * http://en.wikipedia.org/wiki/Box_Drawing
 * ========================================
 *
 * Probably not for now
 *
 * http://en.wikipedia.org/wiki/Miscellaneous_Symbols
 * ==================================================
 *
 * 2605 ★ Black Star
 * 2606 ☆ Star
 *
 * 2610 ☐ Ballot box
 * 2611 ☑ Checked ballot box
 * 2612 ☒ Crossed ballot bow
 *
 *
 * 262F ☯ Yin/Yang == Balance
 *
 * 2295 ⊕ Plus in circle - used as "self"
 * 2640 ♀ Female sign
 * 2641 ♁ Earth
 * 2642 ♂ Male sign
 *
 * 2690 ⚐ White flag
 * 2691 ⚑ Black flag
 *
 * 2696 ⚖ Weighing scales
 * 2698 ⚘ Flower
 *
 * 26A0 ⚠ Warning Sign
 *
 * 26A2 ⚢ Doubled female
 *
 * 26A3 ⚣ Double male
 * 26A4 ⚤ Interlocked male/female
 * 26A5 ⚥ Male and female
 *
 * 26AD ⚭ Married symbol
 *
 * http://www.alanwood.net/unicode/enclosed_alphanumerics.html
 * https://en.wikipedia.org/wiki/Enclosed_Alphanumerics
 * ====================================================
 *
 * Circled numbers
 * 24EA ⓪ Zero
 * 2460 ① One
 * 2461 ②
 * 2462 ③
 * 2463a④
 * 2464 ⑤
 * 2465 ⑥
 * 2466 ⑦
 * 2467 ⑧
 * 2468 ⑨
 * 2469 ⑩
 * 246A ⑪
 * 246B ⑫
 * 246C ⑬
 * 246D ⑭
 * 246E ⑮
 * 246F ⑯
 * 2470 ⑰
 * 2471 ⑱
 * 2472 ⑲
 * 2473 ⑳
 *
 *
 * Numbers in parentheses
 * 2474 ⑴
 * 2475 ⑵
 * 2476 ⑶
 * 2477 ⑷
 * 2478 ⑸
 * 2479 ⑹
 * 247A ⑺
 * 247B ⑻
 * 247C ⑼
 * 247D ⑽
 * 247E ⑾
 * 247D ⑿
 * 2480 ⒀
 * 2481 ⒁
 * 2482 ⒂
 * 2483 ⒃
 * 2484 ⒄
 * 2485 ⒅
 * 2486 ⒆
 * 2487 ⒇
 *
 *
 * Circled numbers (reverse video)
 * https://en.wikipedia.org/wiki/Dingbat
 *
 * 24FF ⓿ Zero
 * 278A ➊ 2776 ❶
 * 278B ➋ 2777 ❷
 * 278C ➌ 2778 ❸
 * 278D ➍ 2779 ❹
 * 278E ➎ 277A ❺
 * 278F ➏ 277B ❻
 * 2790 ➐ 277C ❼
 * 2791 ➑ 277D ❽
 * 2792 ➒ 277E ❾
 * 2793 ➓ 277F ❿
 *
 * https://en.wikipedia.org/wiki/Enclosed_Alphanumerics
 * 24EB ⓫
 * 24EC ⓬
 * 24ED ⓭
 * 24EE ⓮
 * 24EF ⓯
 * 24F0 ⓰
 * 24F1 ⓱
 * 24F2 ⓲
 * 24F3 ⓳
 * 24F4 ⓴
 *
 *
 *
 * http://en.wikipedia.org/wiki/Geometric_Shapes
 * =============================================
 *
 * &#x25b6	▶	Black right
 * &#x25b7	▷	White right
 *
 * &#25C0	◀	Black Left
 * &#25C1	◁	White left
 *
 * &#x25f0	◰	Upper left quadrqnt
 * &#x25f1	◱	Lower left quadrant
 * &#x25f2	◲	Lower right quadrant
 * &#x25f3	◳	Upper right quadrant
 *
 * &#x25a6	▦	Square with crosshatch fill
 *
 * 25B2  ▲ Black Up
 * 25B3  △ White Up
 *
 * &#x25BC	▼	Black down
 * &#x25BD	▽	White down
 *
 * &#x25c9	◉	Fisheye ; japanese bullet
 *
 *
 * http://unicode-table.com/en/#arrows
 *
 * ← 2190
 * ↑
 * →
 * ↓
 * ↔ 2194
 * ↕
 * ↖
 * ↗
 * ↘
 * ↙
 * ↚ 219A
 * ↛
 * ↜
 * ↝
 * ↞ 219E double arrowhead left
 * ↟ 219F
 * ↠ 21A0
 * ↡
 * ↢ 21A2 Feathered
 * ↣ 21A3
 * ↤ 21A3 Straight
 * ↤ 21A4
 * ↦ 21A6
 * ↧ 21A7
 * ↨
 * ↪ 21AA
 * ↩
 *
 *
 * http://unicode-table.com/en/#miscellaneous-technical
 *
 * ⌦ 2326 Right pointing X in box
 * ⌫ 232B Left pointing X in box
 *
 *
 * ⍟ 235F start in circle
 *
 * http://en.wikipedia.org/wiki/Dingbat
 * ====================================
 *
 * 2701 ✁
 * 2702 ✂
 * 2703 ✃
 * 2704 ✄
 * 2705 ✅
 * 2706 ✆
 * 2707 ✇
 * 2708 ✈
 * 2709 ✉
 * 270A ✊
 * 270B ✋
 * 270C ✌
 * 270D ✍
 * U+270E   ✎  Pencil SE
 * U+270F   ✏  Pencil East
 * U+2710   ✐  Pencil NE
 * U+2711   ✑  Right pointer
 * U+2712   ✒  Heavy Right pointer
 * U+2713	✓	Check mark
 * U+2714	✔	Heavy check mark
 * U+2715	✕	Multiplication X
 * U+2716	✖	Heavy multiplication X
 * U+2717	✗	Ballot X
 * U+2718	✘	Heavy ballot X
 * 2719 ✙
 * 271A ✚
 * 271B ✛
 * 271C ✜
 * 271D ✝
 * 271E ✞
 * 271F ✟
 * 2720 ✠
 * 2721 ✡
 * 2722 ✢
 * 2723 ✣
 * 2724 ✤
 * 2725 ✥
 * 2726 ✦
 * 2727 ✧
 * 2728 ✨
 * U+2729	✩	Stress outlined white star
 * U+272A	✪	Circled white star
 * U+272B	✫	Open center black star
 * U+272C	✬	Black center white star
 * U+272D	✭	Outlined black star
 * U+272E	✮	Heavy outlined black star
 * U+272F	✯	Pinwheel star
 * U+2730	✰	Shadowed white star
 * ✱
 * ✲
 * ✳
 * ✴
 * ✵
 * ✶
 * ✷
 * ✸
 * ✹ 2739
 * ✺
 * ✻
 * ✼
 * ✽
 * ✾
 * ✿
 * U+274B	❋	Heavy eight teardrop spoked propeller asterisk
 * U+274C	❌	Cross mark
 * U+274D	❍	Shadowed white circle
 * U+274E	❎	Negative squared cross mark
 *
 * U+2753	❓	Black question mark ornament
 * U+2754	❔	White question mark ornament
 * U+2755	❕	White exclamation mark ornament
 * U+2756	❖	Black diamond minus white X
 * U+2757	❗	Heavy exclamation mark symbol
 *
 * U+2794	➔	Heavy wide-headed rightward arrow
 * U+2798	➘	Heavy south east arrow
 * U+2799	➙	Heavy rightward arrow
 * U+279A	➚	Heavy north east arrow
 * U+279B	➛	Drafting point rightward arrow
 * U+279C	➜	Heavy round-tipped rightward arrow
 * U+279D	➝	Triangle-headed rightward arrow
 * U+279E	➞	Heavy triangle-headed rightward arrow
 * U+279F	➟	Dashed triangle-headed rightward arrow
 * U+27A0	➠	Heavy dashed triangle-headed rightward arrow
 * U+27A1	➡	Black rightward arrow
 *
 * Bottom Eighths:
 * 1/8: 2581
 * 2/8: 2582
 * 3/8: 2583
 * 4/8: 2584
 * 5/8: 2585
 * 6/8: 2586
 * 7/8: 2587
 * 8/8: 2588
 *
 * Left Eighths:
 * 1/8: 258F
 * 2/8: 258E
 * 3/8: 258D
 * 4/8: 258C
 * 5/8: 258B
 * 6/8: 258A
 * 7/8: 25B9
 * 8/8: 2588
 *
 */
@SuppressWarnings("nls")
public class UnicodeChars {

	public static final String MAC_COMMAND_KEY = "\u2318"; // ⌘
	public static final String ALPHA = "\u03b1"; // α
	public static final String BETA = "\u03b2"; // β

	public static final String SIGN__SELF   = "\u2295"; // ⊕ Plus in circle - used as "self"
	public static final String SIGN_FEMALE = "\u2640"; // ♀ Female sign
	public static final String SIGN_MALE   = "\u2642"; // ♂ Male sign
	public static final String SIGN_FEMALE_MALE = "\u26a4"; // ⚤

    public static final String ELLIPSIS = "\u2026"; // …
//  public static final String SEARCH = "\u2315"; // ⌕
    public static final String WHITE_FLAG = "\u2690"; // ⚐
    public static final String BLACK_FLAG = "\u2691"; // ⚑

    public static final String WRIST_WATCH = "\u231A"; // ⌚

    public static final String ALARM_CLOCK = "\u23F0"; // ⏰
    public static final String STOPWATCH   = "\u23F1"; // ⏱
    public static final String CLOCK_FACE  = "\u23F2"; // ⏲
    public static final String HOURGLASS   = "\u23F3"; // ⏳

    public static final String TRIANGLE_L = "\u23F4"; // ⏴
    public static final String TRIANGLE_R = "\u23F5"; // ⏵
    public static final String TRIANGLE_UP = "\u23F6"; // ⏶
    public static final String TRIANGLE_DN = "\u23F7"; // ⏷

    public static final String PLAY_PAUSE  = "\u23F8"; // ⏸
    public static final String PLAY_STOP   = "\u23F9"; // ⏹
    public static final String PLAY_RECORD = "\u23FA"; // ⏺

    public static final String SHAMROCK = "\u2618"; // ☘

    public static final String BLACK_STAR = "\u2605"; // ★
    public static final String WHITE_STAR = "\u2606"; // ☆

    public static final String FLOWER = "\u2698"; // ⚘

    public static final String PLUS = "\u2795"; // ➕

    public static final String CANCEL_CROSS = "\u2718"; // ✘
    public static final String CONFIRM_TICK = "\u2713"; // ✓

    public static final String OPEN_CENTRE_CROSS = "\u271B"; // ✛

    public static final String BLACK_CIRCLE = "\u25CF"; // ●
    public static final String PENCIL = "\u270E"; // ✎

    public static final String CAUTION = "\u26A0"; // ⚠
    public static final String SKULL_XBONE = "\u2620"; // ☠

    public static final String DIRN_UP    = "\u2191"; // ↑
    public static final String DIRN_DOWN  = "\u2193"; // ↓
    public static final String DIRN_LEFT  = "\u2190"; // ←
    public static final String DIRN_RIGHT = "\u2192"; // →

    public static final String DOTTED_CIRCLE = "\u25CC"; // ◌

    public static final String DOTTED_CIRCLE_3 = "\u25CC\u25CC\u25CC";
    // ✓ 2713 Checkmark
    // ✔ 2714 Heavy checkmark
    // ✕ 2715 Multiply
    // ✖ 2716
    // ✗ 2717 Ballot X
    // ✘ 2718 Heavy ballot X

    // ☐ 2610 Ballot box
    // ☑ 2611 Ballot box with check
    // ☒ 2612 Ballot box with cross

    // ⬚ 2B1A Dotted square


    // ▲ 25B2 black up triangle
    // △ 25B3 white up triangle
    // ▶ 25B6
    // ▷ 25B7
    // ▼ 25BC
    // ▽ 25B#
    // ◀ 25C0
    // ◁ 25C1 white left triangle

    // ◆ 25C6 black diamond
    // ◇ 25C7 white  diamond
    // ◈ 25C8 black diamond in white diamond
    // ◉ 25C9 fisheye (bullet in circle)
    // ◊ 25CA tall diamond
    // ○ 25CB white circle
    // ◌ 25CC dotted circle
    // ◍ 25CD striped circle
    // ◎ 25CE bullseye (two circles)
    // ● 25CF black circle
    // ◯ 25EF large circle

    // ⿰ 2FF0 L/R rects
    // ⿱ 2FF1 U/D rects
    // ⿲ 2FF2
    // ⿳
    // ⿴ 2FF4
    // ⿵ 2FF5
    // ⿶
    // ⿷
    // ⿸ 2FF8 LR
    // ⿹ 2FF9 LL
    // ⿺ 2FFA UR
    // ⿻ 2FFB


    // ☠ 2620 skull and crossbones

    public enum Number {
        N_00("\u24EA", "\u24FF", ""),   // ⓪, ⓿, NO-CHAR
        N_01("\u2460", "\u278A", "\u2474"), // ①, ➊  ⑴
        N_02("\u2461", "\u278B", "\u2475"), // ②, ➋  ⑵
        N_03("\u2462", "\u278C", "\u2476"), // ③, ➌  ⑶
        N_04("\u2463", "\u278D", "\u2477"), // ④, ➍  ⑷
        N_05("\u2464", "\u278E", "\u2478"), // ⑤, ➎,  ⑸
        N_06("\u2465", "\u278F", "\u2479"), // ⑥, ➏,  ⑹
        N_07("\u2466", "\u2790", "\u247A"), // ⑦, ➐,  ⑺
        N_08("\u2467", "\u2791", "\u247B"), // ⑧, ➑,  ⑻
        N_09("\u2468", "\u2792", "\u247C"), // ⑨, ➒,  ⑼
        N_10("\u2469", "\u2793", "\u247D"), // ⑩, ➓,  ⑽
        N_11("\u246A", "\u24EB", "\u247E"), // ⑪, ⓫,  ⑾
        N_12("\u246B", "\u24EC", "\u247D"), // ⑫, ⓬,  ⑿
        N_13("\u246C", "\u24ED", "\u2480"), // ⑬, ⓭,  ⒀
        N_14("\u246D", "\u24EE", "\u2481"), // ⑭, ⓮,  ⒁
        N_15("\u246E", "\u24EF", "\u2482"), // ⑮, ⓯,  ⒂
        N_16("\u246F", "\u24F0", "\u2483"), // ⑯, ⓰,  ⒃
        N_17("\u2470", "\u24F1", "\u2484"), // ⑰, ⓱,  ⒄
        N_18("\u2471", "\u24F2", "\u2485"), // ⑱, ⓲,  ⒅
        N_19("\u2472", "\u24F3", "\u2486"), // ⑲, ⓳,  ⒆
        N_20("\u2473", "\u24F4", "\u2487"), // ⑳, ⓴,  ⒇
        N_nn("\u24DD", "\u272A", "\u24A9")  // ⓝ, ✪,  ⒩

        // https://en.wikipedia.org/wiki/Enclosed_CJK_Letters_and_Months
        // gives 35 through 50
        // 3251 ㉑
        // 3252 ㉒
        // 3253 ㉓
        // 3254 ㉔
        // 3255 ㉕
        // 3256 ㉖
        // 3257 ㉗
        // 3258 ㉘
        // 3259 ㉙
        // 325A ㉚
        // 325B ㉛
        // 325C ㉜
        // 325D ㉝
        // 325E ㉞
        // 325F ㉟
        // --
        // 32B1 ㊱
        // :
        // 32BF ㊿
        ;

        public final String positive;
        public final String negative;
        public final String parenthesis;

        Number(String p, String n, String paren) {
            positive = p;
            negative = n;
            parenthesis = paren;
        }

        public static final String N_IN_PARENS = "\u24A9"; // ⒩

        public static final String N_POSITIVE = "\u24dd"; // ⓝ

        public static final String N_NEGATIVE = "\u272A"; // ✪ Circled White Star
    }

    // These give
    public enum Eighth {
        ZERO ("_",      "_"),
        ONE  ("\u2581", "\u258F"), // ▁ ▏
        TWO  ("\u2582", "\u258E"), // ▂ ▎
        THREE("\u2583", "\u258D"), // ▃ ▍
        FOUR ("\u2584", "\u258C"), // ▄ ▌
        FIVE ("\u2585", "\u258B"), // ▅ ▋
        SIX  ("\u2586", "\u258A"), // ▇ ▊
        SEVEN("\u2587", "\u2589"), // ▇ ▉
        EIGHT("\u2588", "\u2588"), // █ █
        ;

        String bottom;
        String left;
        Eighth(String b, String l) {
            bottom = b;
            left = l;
        }
    }
}
