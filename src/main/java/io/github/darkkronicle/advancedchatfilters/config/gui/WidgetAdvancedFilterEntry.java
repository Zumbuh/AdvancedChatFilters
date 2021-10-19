package io.github.darkkronicle.advancedchatfilters.config.gui;

import com.mojang.blaze3d.systems.RenderSystem;
import fi.dy.masa.malilib.gui.GuiBase;
import fi.dy.masa.malilib.gui.button.ButtonBase;
import fi.dy.masa.malilib.gui.button.ButtonGeneric;
import fi.dy.masa.malilib.gui.button.ButtonOnOff;
import fi.dy.masa.malilib.gui.button.IButtonActionListener;
import fi.dy.masa.malilib.gui.interfaces.ITextFieldListener;
import fi.dy.masa.malilib.gui.widgets.WidgetBase;
import fi.dy.masa.malilib.gui.widgets.WidgetListEntryBase;
import fi.dy.masa.malilib.gui.wrappers.TextFieldWrapper;
import fi.dy.masa.malilib.render.RenderUtils;
import fi.dy.masa.malilib.util.KeyCodes;
import fi.dy.masa.malilib.util.StringUtils;
import io.github.darkkronicle.advancedchatcore.config.gui.widgets.WidgetIntBox;
import io.github.darkkronicle.advancedchatcore.util.ColorUtil;
import io.github.darkkronicle.advancedchatfilters.FiltersHandler;
import io.github.darkkronicle.advancedchatfilters.config.AdvancedFilter;
import io.github.darkkronicle.advancedchatfilters.config.FiltersConfigStorage;
import net.fabricmc.api.EnvType;
import net.fabricmc.api.Environment;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.util.math.MatrixStack;

import java.util.Collections;

@Environment(EnvType.CLIENT)
public class WidgetAdvancedFilterEntry extends WidgetListEntryBase<AdvancedFilter> {

    private final WidgetListAdvancedFilters parent;
    private final boolean isOdd;
    private final int buttonStartX;
    private final AdvancedFilter filter;
    private final TextFieldWrapper<WidgetIntBox> num;

    public WidgetAdvancedFilterEntry(int x, int y, int width, int height, boolean isOdd, AdvancedFilter filter, int listIndex, WidgetListAdvancedFilters parent) {
        super(x, y, width, height, filter, listIndex);
        this.parent = parent;
        this.isOdd = isOdd;
        this.filter = filter;

        y += 1;

        int pos = x + width - 2;
        WidgetIntBox num = new WidgetIntBox(pos - 40, y, 40, 20, MinecraftClient.getInstance().textRenderer);
        num.setText(filter.getOrder().toString());
        num.setApply(() -> {
            Integer order = num.getInt();
            if (order == null) {
                order = 0;
            }
            this.filter.setOrder(order);
            Collections.sort(FiltersConfigStorage.FILTERS);
            FiltersHandler.getInstance().loadFilters();
            this.parent.refreshEntries();
        });
        this.num = new TextFieldWrapper<>(num, new ITextFieldListener<WidgetIntBox>() {
            @Override
            public boolean onTextChange(WidgetIntBox textField) {
                return false;
            }

            @Override
            public boolean onGuiClosed(WidgetIntBox textField) {
                Integer order = num.getInt();
                if (order == null) {
                    order = 0;
                }
                filter.setOrder(order);
                Collections.sort(FiltersConfigStorage.FILTERS);
                return false;
            }
        });
        this.parent.addTextField(this.num);
        pos -= num.getWidth() + 2;
        pos -= addOnOffButton(pos, y, ButtonListener.Type.ACTIVE, filter.getActive().config.getBooleanValue());

        buttonStartX = pos;
    }

    private int addOnOffButton(int xRight, int y, ButtonListener.Type type, boolean isCurrentlyOn) {
        ButtonOnOff button = new ButtonOnOff(xRight, y, -1, true, type.translate, isCurrentlyOn);
        this.addButton(button, new ButtonListener(type, this));

        return button.getWidth() + 1;
    }

    @Override
    public void render(int mouseX, int mouseY, boolean selected, MatrixStack matrixStack) {
        RenderUtils.color(1f, 1f, 1f, 1f);

        // Draw a lighter background for the hovered and the selected entry
        if (selected || this.isMouseOver(mouseX, mouseY)) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, ColorUtil.WHITE.withAlpha(150).color());
        } else if (this.isOdd) {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, ColorUtil.WHITE.withAlpha(70).color());
        } else {
            RenderUtils.drawRect(this.x, this.y, this.width, this.height, ColorUtil.WHITE.withAlpha(50).color());
        }
        String name = this.filter.getName().config.getStringValue();
        this.drawString(this.x + 4, this.y + 7, ColorUtil.WHITE.color(), name, matrixStack);

        RenderUtils.color(1f, 1f, 1f, 1f);
        RenderSystem.disableBlend();

        this.drawTextFields(mouseX, mouseY, matrixStack);

        super.render(mouseX, mouseY, selected, matrixStack);

        RenderUtils.disableDiffuseLighting();
    }

    private static class ButtonListener implements IButtonActionListener {

        private final Type type;
        private final WidgetAdvancedFilterEntry parent;

        public ButtonListener(Type type, WidgetAdvancedFilterEntry parent) {
            this.parent = parent;
            this.type = type;
        }

        @Override
        public void actionPerformedWithButton(ButtonBase button, int mouseButton) {
            if (type == Type.ACTIVE) {
                this.parent.filter.getActive().config.setBooleanValue(!this.parent.filter.getActive().config.getBooleanValue());
                FiltersHandler.getInstance().loadFilters();
                parent.parent.refreshEntries();
            }
        }

        public enum Type {
            ACTIVE("active");

            private final String translate;

            Type(String name) {
                this.translate = translate(name);
            }

            private static String translate(String key) {
                return "advancedchatfilters.config.filtermenu." + key;
            }

            public String getDisplayName() {
                return StringUtils.translate(translate);
            }

        }

    }

    @Override
    protected boolean onKeyTypedImpl(int keyCode, int scanCode, int modifiers) {
        if (this.num != null && this.num.isFocused()) {
            if (keyCode == KeyCodes.KEY_ENTER) {
                this.num.getTextField().getApply().run();
                return true;
            } else {
                return this.num.onKeyTyped(keyCode, scanCode, modifiers);
            }
        }

        return false;
    }

    @Override
    protected boolean onCharTypedImpl(char charIn, int modifiers) {
        if (this.num != null && this.num.onCharTyped(charIn, modifiers)) {
            return true;
        }

        return super.onCharTypedImpl(charIn, modifiers);
    }

    @Override
    protected boolean onMouseClickedImpl(int mouseX, int mouseY, int mouseButton) {
        if (super.onMouseClickedImpl(mouseX, mouseY, mouseButton)) {
            return true;
        }

        boolean ret = false;

        if (this.num != null) {
            ret = this.num.getTextField().mouseClicked(mouseX, mouseY, mouseButton);
        }

        if (!this.subWidgets.isEmpty()) {
            for (WidgetBase widget : this.subWidgets) {
                ret |= widget.isMouseOver(mouseX, mouseY) && widget.onMouseClicked(mouseX, mouseY, mouseButton);
            }
        }

        return ret;
    }

    protected void drawTextFields(int mouseX, int mouseY, MatrixStack matrixStack) {
        if (this.num != null) {
            this.num.getTextField().render(matrixStack, mouseX, mouseY, 0f);
        }
    }

}