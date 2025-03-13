package moe.mukjep.fbsr.gui

import com.demod.factorio.FactorioData
import moe.mukjep.fbsr.gui.feature.GUIPipeFeature.Companion.dragLines
import moe.mukjep.fbsr.gui.feature.GUIPipeFeature.Companion.full
import moe.mukjep.fbsr.gui.feature.GUISliceFeature.Companion.inner
import moe.mukjep.fbsr.gui.feature.GUISliceFeature.Companion.outer
import moe.mukjep.fbsr.gui.feature.GUIStaticFeature
import java.awt.Color
import java.awt.Font

object GUIStyle {

    @JvmField
    val FONT_BP_REGULAR = "__core__/fonts/Lilittium-Regular.ttf".createFont()
    @JvmField
    val FONT_BP_BOLD = "__core__/fonts/Lilittium-Bold.ttf".createFont()
    @JvmField
    val FONT_BP_COLOR = Color(0xffe6c0)

    // TODO load these details from data.raw.gui-styles.default
    const val DEFAULT_TILESET = "__core__/graphics/gui-new.png"

    // frame
    @JvmField
    val FRAME_INNER = inner(
        DEFAULT_TILESET,  //
        GUIBox(0, 0, 17, 17), GUISpacing(8, 8, 8, 8)
    )
    // inside_shallow_frame
    @JvmField
    val FRAME_OUTER = outer(
        DEFAULT_TILESET,  //
        GUIBox(17, 0, 17, 17), GUISpacing(8, 8, 8, 8)
    )

    // research_progress_inner_frame_inactive / technology_card_frame /
    // table_with_selection
    @JvmField
    val FRAME_DARK_INNER = inner(
        DEFAULT_TILESET,  //
        GUIBox(34, 0, 17, 17), GUISpacing(8, 8, 8, 8)
    )
    // table_with_selection
    @JvmField
    val FRAME_DARK_OUTER = outer(
        DEFAULT_TILESET,  //
        GUIBox(51, 0, 17, 17), GUISpacing(8, 8, 8, 8)
    )
    @JvmField
    val FRAME_DARK_BUMP_OUTER = outer(
        DEFAULT_TILESET,  //
        GUIBox(282, 17, 17, 17), GUISpacing(8, 8, 8, 8)
    )

    // train_with_minimap_frame, shallow_frame, bonus_card_frame,
    // mods_explore_results_table, research_progress_inner_frame_active
    @JvmField
    val FRAME_LIGHT_INNER = inner(
        DEFAULT_TILESET,  //
        GUIBox(68, 0, 17, 17), GUISpacing(8, 8, 8, 8)
    )
    // deep_scroll_pane, control_settings_section_frame, mod_thumbnail_image,
    // scroll_pane_in_shallow_frame,
    // list_box_in_shallow_frame_under_subheader_scroll_pane, tab_scroll_pane,
    // deep_frame_in_shallow_frame, text_holding_scroll_pane,
    // tab_deep_frame_in_entity_frame, shallow_frame_in_shallow_frame,
    // list_box_in_shallow_frame_scroll_pane
    @JvmField
    val FRAME_LIGHT_OUTER = outer(
        DEFAULT_TILESET,  //
        GUIBox(85, 0, 17, 17), GUISpacing(8, 8, 8, 8)
    )

    // Probably not used correctly...
    @JvmField
    val FRAME_TAB = inner(
        DEFAULT_TILESET, GUIBox(448, 103, 17, 17),
        GUISpacing(16, 8, 0, 8)
    )

    @JvmField
    val PIPE = full(DEFAULT_TILESET, GUIBox(0, 40, 120, 8))
    @JvmField
    val DRAG_LINES = dragLines(DEFAULT_TILESET, GUIBox(192, 8, 24, 8))

    @JvmField
    val ITEM_SLOT = GUIStaticFeature(DEFAULT_TILESET, GUIBox(0, 736, 80, 80))

    private fun String.createFont(): Font {
        return Font.createFont(Font.TRUETYPE_FONT, FactorioData.getModResource(this).get())
    }
}
