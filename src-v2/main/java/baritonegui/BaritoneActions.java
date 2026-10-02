package baritonegui;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

/**
 * Static definition of Baritone command categories, plus the settings categories
 * which are built at runtime from the ACTUAL Baritone settings (so every setting is
 * present and nothing is hard-coded / omitted).
 *
 * <p>Command syntax follows the official Baritone USAGE.md / Settings docs:
 *   - settings use the {@code set} command: {@code set <name> <value>},
 *     {@code set toggle <name>}, {@code set reset <name>}, {@code set <name>} (view),
 *     {@code set modified}.
 *   - waypoints use {@code wp}: {@code wp save <tag> <name>}, {@code wp goal <tag>}.
 *   - {@code follow} forms: {@code follow player <name>}, {@code follow players},
 *     {@code follow entity <type>}, {@code follow entities}.
 *
 * <p>Every action has a stable {@code id} (used for favorites), a friendly {@code display},
 * a {@code command} template (WITHOUT leading '#'/'.') with %N placeholders, and optional
 * {@link Param}s. A Param may carry a {@code kind} driving autocompletion.
 */
public final class BaritoneActions {

    /** A parameter that needs a custom input box. */
    public static class Param {
        public final String label;
        public final String placeholder;
        public final String kind;

        public Param(String label, String placeholder) {
            this(label, placeholder, null);
        }

        public Param(String label, String placeholder, String kind) {
            this.label = label;
            this.placeholder = placeholder;
            this.kind = kind;
        }
    }

    public static class Action {
        public final String id;
        public final String display;
        public final String command;
        public final Param[] params;
        /** Translation key (baritonegui.action.<id>); runtime setting actions have
         *  no entry and fall back to {@code display} (the raw setting name). */
        public final String key;

        public Action(String id, String display, String command, Param... params) {
            this.id = id;
            this.display = display;
            this.command = command;
            this.params = params;
            this.key = "baritonegui.action." + id;
        }

        public String build(List<String> values) {
            String c = command;
            for (int i = 0; i < values.size(); i++) {
                c = c.replace("%" + i, values.get(i).trim());
            }
            return c;
        }
    }

    public static class Category {
        public final String key;
        public final String name;
        public final Action[] actions;

        public Category(String key, String name, Action... actions) {
            this.key = key;
            this.name = name;
            this.actions = actions;
        }
    }

    // ---- Shorthand builders --------------------------------------------------
    private static Param P(String label, String placeholder) {
        return new Param(label, placeholder, null);
    }

    private static Param P(String label, String placeholder, String kind) {
        return new Param(label, placeholder, kind);
    }

    private static Action cmd(String id, String display, String command, Param... p) {
        return new Action(id, display, command, p);
    }

    private static Action toggle(String id, String display, String setting) {
        return new Action(id, display, "set toggle " + setting);
    }

    private static Action num(String id, String display, String setting, String def) {
        return new Action(id, display, "set " + setting + " %0", new Param("值", def));
    }

    // ---- Static COMMAND categories (the non-settings commands) ----

    private static final Category[] COMMAND_CATEGORIES = new Category[]{

            // 1) 移动 / 前往
            new Category("baritonegui.cat.move", "移动前往",
                    cmd("go_goto", "前往坐标 (goto x y z)", "goto %0 %1 %2",
                            P("X", "坐标X"), P("Y", "坐标Y"), P("Z", "坐标Z")),
                    cmd("go_goto2", "前往目标 (goto x z / 方块 / portal)", "goto %0",
                            P("目标", "x z 或 diamond_ore 或 portal")),
                    cmd("go_goal", "设置目标 (goal x y z)", "goal %0 %1 %2",
                            P("X", "坐标X"), P("Y", "坐标Y"), P("Z", "坐标Z")),
                    cmd("go_goal2", "目标 (goal x z / y)", "goal %0",
                            P("目标", "x z 或 y")),
                    cmd("go_thisway", "沿视线前进 (thisway 距离)", "thisway %0",
                            P("距离", "向前步数")),
                    cmd("go_come", "朝镜头走 (come)", "come"),
                    cmd("go_follow_p", "跟随玩家 (follow player 名)", "follow player %0",
                            P("玩家名", "玩家名", "player")),
                    cmd("go_follow_ps", "跟随所有玩家 (follow players)", "follow players"),
                    cmd("go_follow_e", "跟随实体类 (follow entity 类型)", "follow entity %0",
                            P("实体类型", "pig / zombie")),
                    cmd("go_follow_es", "跟随实体 (follow entities)", "follow entities"),
                    cmd("go_jump", "原地起跳 (jump)", "jump"),
                    cmd("go_axis", "对齐轴 (axis)", "axis"),
                    cmd("go_invert", "反转目标 (invert)", "invert"),
                    cmd("go_surface", "到地表 (surface)", "surface"),
                    cmd("go_top", "到顶部 (top)", "top")
            ),

            // 2) 寻路 / 隧道
            new Category("baritonegui.cat.path", "寻路隧道",
                    cmd("path_path", "规划路线并前往 (path)", "path"),
                    cmd("path_tunnel", "直线隧道 (tunnel)", "tunnel"),
                    cmd("path_tunnel2", "挖掘区域 (tunnel 高 宽 深)", "tunnel %0 %1 %2",
                            P("高度", "隧道高"), P("宽度", "隧道宽"), P("深度", "隧道深")),
                    cmd("path_worm", "虫洞挖掘 (worm 长 高 宽)", "worm %0 %1 %2",
                            P("长度", "隧道长"), P("高度", "隧道高"), P("宽度", "隧道宽")),
                    cmd("path_explore", "随机探索 (explore)", "explore"),
                    cmd("path_explore2", "探索坐标 (explore x z)", "explore %0 %1",
                            P("X", "坐标X"), P("Z", "坐标Z")),
                    cmd("path_explorefilter", "探索过滤 (explorefilter 文件)", "explorefilter %0",
                            P("过滤文件", "filter.json"))
            ),

            // 3) 暂停 / 取消
            new Category("baritonegui.cat.control", "暂停取消",
                    cmd("ctrl_stop", "停止 (stop)", "stop"),
                    cmd("ctrl_cancel", "取消 (cancel)", "cancel"),
                    cmd("ctrl_forcecancel", "强制取消 (forcecancel)", "forcecancel"),
                    cmd("ctrl_pause", "暂停/继续 (pause)", "pause")
            ),

            // 4) 挖掘 / 采集
            new Category("baritonegui.cat.mine", "挖掘采集",
                    cmd("mine_mine", "挖掘方块 (mine 方块)", "mine %0",
                            P("方块ID", "minecraft:diamond_ore", "block")),
                    cmd("mine_farm", "农场模式 (farm)", "farm"),
                    cmd("mine_farm2", "农场距离 (farm 距离/路点)", "farm %0",
                            P("距离/路点", "扫描半径 或 路点名")),
                    cmd("mine_find", "寻找方块 (find 方块 范围)", "find %0 %1",
                            P("方块ID", "minecraft:iron_ore", "block"), P("范围", "搜索半径")),
                    cmd("mine_findfilter", "寻找过滤 (findfilter)", "findfilter"),
                    cmd("mine_surround", "包围挖掘 (surround 方块)", "surround %0",
                            P("方块ID", "minecraft:obsidian", "block")),
                    cmd("mine_repack", "重新缓存区块 (repack)", "repack")
            ),

            // 5) 建造 / 选区
            new Category("baritonegui.cat.build", "建造选区",
                    cmd("build_build", "建造蓝图 (build 名称)", "build %0",
                            P("文件名", "schematic名(不含后缀)", "schematic")),
                    cmd("build_build0", "以脚下为原点建造 (build)", "build"),
                    cmd("build_sel", "区域选择 (sel 模式)", "sel %0",
                            P("模式", "pos1/pos2/chunk/clear", "selmode")),
                    cmd("build_wand", "魔杖选取 (wand)", "wand"),
                    cmd("build_schematica", "建造Schematica当前 (schematica)", "schematica"),
                    cmd("build_reloadall", "重载世界缓存 (reloadall)", "reloadall")
            ),

            // 6) 路点 (Baritone 当前命令为 wp)
            new Category("baritonegui.cat.waypoint", "路点",
                    cmd("wp_save", "保存路点 (wp save 标签 名)", "wp save %0 %1",
                            P("标签", "user/home/death", "tag"), P("名称", "路点名", "tag")),
                    cmd("wp_goal", "前往路点 (wp goal 标签)", "wp goal %0",
                            P("标签", "death/home", "tag")),
                    cmd("wp_goaldeath", "列出死亡点 (wp goal death)", "wp goal death")
            ),

            // 7) 工具 / 信息 / 设置命令
            new Category("baritonegui.cat.tools", "工具信息",
                    cmd("tool_help", "帮助 (help)", "help"),
                    cmd("tool_version", "版本 (version)", "version"),
                    cmd("tool_eta", "预计到达 (eta)", "eta"),
                    cmd("tool_proc", "进程信息 (proc)", "proc"),
                    cmd("tool_repack", "缓存区块 (repack)", "repack"),
                    cmd("tool_render", "修复渲染 (render)", "render"),
                    cmd("tool_reloadall", "重载缓存 (reloadall)", "reloadall"),
                    cmd("tool_saveall", "保存缓存 (saveall)", "saveall"),
                    cmd("tool_gc", "回收内存 (gc)", "gc"),
                    cmd("tool_surface", "到地表 (surface)", "surface"),
                    cmd("tool_top", "到顶部 (top)", "top"),
                    cmd("tool_blacklist", "屏蔽方块 (blacklist)", "blacklist"),
                    cmd("tool_invert", "反转目标 (invert)", "invert"),
                    cmd("tool_click", "点击屏幕 (click)", "click"),
                    cmd("tool_set", "设置数值 (set 名称 值)", "set %0 %1",
                            P("设置名", "allowBreak", "setting"), P("值", "true/false 或数字", "value")),
                    cmd("tool_setview", "查看设置 (set 名称)", "set %0",
                            P("设置名", "allowBreak", "setting")),
                    cmd("tool_settoggle", "开关设置 (set toggle 名称)", "set toggle %0",
                            P("设置名", "allowBreak", "setting")),
                    cmd("tool_setreset", "重置设置 (set reset 名称)", "set reset %0",
                            P("设置名", "allowBreak", "setting")),
                    cmd("tool_setmodified", "已改设置 (set modified)", "set modified"),
                    cmd("tool_settings", "设置列表 (settings)", "settings")
            )
    };

    // ---- Runtime settings categories (built from the live Baritone settings) ----

    private static final Category SETTINGS_FALLBACK = new Category("baritonegui.cat.settings", "设置调整",
            toggle("set_allowBreak", "允许破坏 (allowBreak)", "allowBreak"),
            toggle("set_allowPlace", "允许放置 (allowPlace)", "allowPlace"),
            toggle("set_allowSprint", "允许疾跑 (allowSprint)", "allowSprint"),
            toggle("set_autoTool", "自动选工具 (autoTool)", "autoTool"),
            toggle("set_allowParkour", "允许跑酷 (allowParkour)", "allowParkour"),
            toggle("set_freeLook", "自由视角 (freeLook)", "freeLook"),
            toggle("set_legitMine", "合法挖掘 (legitMine)", "legitMine"),
            toggle("set_renderPath", "渲染路径 (renderPath)", "renderPath"),
            toggle("set_chunkCaching", "区块缓存 (chunkCaching)", "chunkCaching"),
            toggle("set_echoCommands", "回显命令 (echoCommands)", "echoCommands"),
            num("set_axisHeight", "轴高度 (axisHeight)", "axisHeight", "120"),
            num("set_costHeuristic", "成本启发值 (costHeuristic)", "costHeuristic", "3.563"),
            num("set_primaryTimeoutMS", "规划超时ms (primaryTimeoutMS)", "primaryTimeoutMS", "500")
    );

    private static final Category ELYTRA_FALLBACK = new Category("baritonegui.cat.elytra", "鞘翅飞行",
            cmd("elytra_cmd", "鞘翅自动飞行 (elytra)", "elytra"),
            cmd("elytra_goal", "设置目标 (goal x y z)", "goal %0 %1 %2",
                    P("X", "坐标X"), P("Y", "坐标Y"), P("Z", "坐标Z")),
            toggle("set_elytraAllowEmergencyLand", "紧急降落 (elytraAllowEmergencyLand)", "elytraAllowEmergencyLand"),
            toggle("set_elytraAutoSwap", "自动换鞘翅 (elytraAutoSwap)", "elytraAutoSwap"),
            toggle("set_elytraFreeLook", "自由视角 (elytraFreeLook)", "elytraFreeLook"),
            toggle("set_elytraSmoothLook", "平滑视角 (elytraSmoothLook)", "elytraSmoothLook"),
            toggle("set_elytraPredictTerrain", "预测地形 (elytraPredictTerrain)", "elytraPredictTerrain"),
            toggle("set_elytraRenderSimulation", "渲染模拟 (elytraRenderSimulation)", "elytraRenderSimulation"),
            num("set_elytraFireworkSpeed", "烟花速度 (elytraFireworkSpeed)", "elytraFireworkSpeed", "1.2"),
            num("set_elytraMinDurability", "最小耐久 (elytraMinDurability)", "elytraMinDurability", "5")
    );

    // Reflection helpers (cached). Populated lazily; empty if Baritone is absent.
    private static Map<String, Boolean> BOOL_VALS;
    private static List<String> ALL_NAMES;

    /**
     * Returns the live on/off state of every Baritone boolean setting.
     *
     * <p>Recomputed on every call. The previous implementation cached the map in a
     * static field, which lagged Baritone's actual state by one change — the GUI
     * would then show the OPPOSITE of the chat confirmation (chat says "Toggled … to
     * true" while the button stayed "禁用"). Recomputing live guarantees the GUI
     * state always matches Baritone.
     */
    public static Map<String, Boolean> booleanValues() {
        compute();
        return BOOL_VALS;
    }

    public static List<String> allSettingNames() {
        compute();
        return ALL_NAMES;
    }

    /** Live on/off state of a single named boolean setting (null if unknown). */
    public static Boolean booleanValue(String name) {
        compute();
        return BOOL_VALS.get(name);
    }

    /** Force a fresh read of the live Baritone settings. */
    public static void refresh() {
        compute();
    }

    /** Resolve the live Baritone {@code Settings} instance.
     *
     * <p>{@code IBaritone} does NOT expose a {@code settings()} method, and Baritone's
     * concrete main class (e.g. the obfuscated {@code baritone.a}) is renamed per
     * build. The reliable, version-stable accessor is a parameterless {@code static}
     * method returning {@code baritone.api.Settings} (Baritone's global settings
     * accessor). We locate it by return type rather than by name, and fall back to a
     * field of that type. Returns null if Baritone is absent or unreachable. */
    public static Object settingsContainer() {
        try {
            Class<?> api = Class.forName("baritone.api.BaritoneAPI");
            Object provider = api.getMethod("getProvider").invoke(null);
            Object primary = provider.getClass().getMethod("getPrimaryBaritone").invoke(provider);
            Class<?> primaryClass = primary.getClass();
            Class<?> settingsClass = Class.forName("baritone.api.Settings");
            // 1) static accessor: public static Settings <name>()
            for (Class<?> c = primaryClass; c != null; c = c.getSuperclass()) {
                for (Method m : c.getDeclaredMethods()) {
                    if (Modifier.isStatic(m.getModifiers())
                            && m.getParameterCount() == 0
                            && settingsClass.equals(m.getReturnType())) {
                        m.setAccessible(true);
                        return m.invoke(null);
                    }
                }
            }
            // 2) fallback: a field of type Settings on the primary instance
            for (Class<?> c = primaryClass; c != null; c = c.getSuperclass()) {
                for (Field f : c.getDeclaredFields()) {
                    if (settingsClass.equals(f.getType())) {
                        f.setAccessible(true);
                        return f.get(primary);
                    }
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    private static synchronized void compute() {
        Map<String, Boolean> bv = new HashMap<>();
        List<String> all = new ArrayList<>();
        try {
            Object container = settingsContainer();
            if (container != null) {

            // Collect every Setting by scanning the fields of the live Settings
            // instance (type baritone.api.Settings$Setting) instead of relying on a
            // fixed container field name (allSettings / byLowerName) that differs
            // between Baritone builds. This is what guarantees the GUI lists ALL
            // settings and reads their real values on every supported version.
            List<?> settings = collectSettings(container);
            if (settings != null) {
                Set<String> seen = new HashSet<>();
                for (Object s : settings) {
                    try {
                        String name = (String) s.getClass().getMethod("getName").invoke(s);
                        if (!seen.add(name)) continue; // skip duplicate declarations
                        // Prefer get() (the api-defined method that returns the
                        // effective current value); fall back to getValue() for older
                        // Baritone builds.
                        Object v = readValue(s);
                        all.add(name);
                        if (v instanceof Boolean) bv.put(name, (Boolean) v);
                    } catch (Exception ignored) {
                        // Skip one un-reflectable setting instead of aborting the
                        // whole loop (an early failure would drop every later setting
                        // and render them all "禁用").
                    }
                }
            }
            }
        } catch (Exception ignored) {
        }
        BOOL_VALS = bv;
        ALL_NAMES = all;
    }

    /** Collect every Baritone Setting object by scanning the fields of the live
     *  Settings instance, rather than depending on a fixed container field name
     *  (allSettings / byLowerName) that differs between Baritone builds. Any field
     *  whose type is {@code baritone.api.Settings$Setting} IS a setting. This works
     *  on every supported Baritone version (1.20 … 26.x) because that api type and
     *  its get()/getName() methods are stable. */
    private static List<?> collectSettings(Object container) {
        List<Object> result = new ArrayList<>();
        Class<?> settingClass;
        try {
            settingClass = Class.forName("baritone.api.Settings$Setting");
        } catch (Exception e) {
            return result;
        }
        for (Class<?> c = container.getClass(); c != null; c = c.getSuperclass()) {
            Field[] fields;
            try {
                fields = c.getDeclaredFields();
            } catch (Exception e) {
                continue;
            }
            for (Field f : fields) {
                if (!settingClass.isAssignableFrom(f.getType())) continue;
                f.setAccessible(true);
                try {
                    Object v = f.get(container);
                    if (v != null) result.add(v);
                } catch (Exception ignored) {
                }
            }
        }
        return result;
    }

    /** Read a setting's current live value. Prefer the public {@code value} field
     *  (which holds the live value and does NOT depend on a getter method being
     *  invokable — some Baritone builds throw when reflected via get()/getValue(),
     *  which previously made every boolean read come back null and every toggle
     *  show "禁用"). Fall back to get()/getValue() for robustness. */
    private static Object readValue(Object setting) {
        try {
            Field f = setting.getClass().getField("value");
            return f.get(setting);
        } catch (Exception ignored) {
        }
        for (String m : new String[]{"get", "getValue"}) {
            try {
                return setting.getClass().getMethod(m).invoke(setting);
            } catch (Exception ignored) {
            }
        }
        return null;
    }

    /** Returns all categories: the static command categories followed by the live
     *  settings categories (设置调整 = every non-elytra setting; 鞘翅飞行 = elytra* + elytra command).
     *  Falls back to the curated categories if Baritone settings cannot be read. */
    public static Category[] allCategories() {
        List<Category> cats = new ArrayList<>(Arrays.asList(COMMAND_CATEGORIES));

        List<String> names = allSettingNames();
        if (names.isEmpty()) {
            // Fallback: curated settings (no elytra split, still usable).
            cats.add(SETTINGS_FALLBACK);
            cats.add(ELYTRA_FALLBACK);
            return cats.toArray(new Category[0]);
        }

        List<Action> elytra = new ArrayList<>();
        List<Action> others = new ArrayList<>();
        elytra.add(cmd("elytra_cmd", "鞘翅自动飞行 (elytra)", "elytra"));
        elytra.add(cmd("elytra_goal", "设置目标 (goal x y z)", "goal %0 %1 %2",
                P("X", "坐标X"), P("Y", "坐标Y"), P("Z", "坐标Z")));
        for (String name : names) {
            boolean isElytra = name.toLowerCase(Locale.ROOT).startsWith("elytra");
            boolean isBool = booleanValues().containsKey(name);
            Action a;
            if (isBool) {
                a = toggle("set_" + name, name, name);
            } else {
                a = num("set_" + name, name, name, "值");
            }
            if (isElytra) elytra.add(a);
            else others.add(a);
        }
        cats.add(new Category("baritonegui.cat.settings", "设置调整", others.toArray(new Action[0])));
        cats.add(new Category("baritonegui.cat.elytra", "鞘翅飞行", elytra.toArray(new Action[0])));
        return cats.toArray(new Category[0]);
    }
}
