package net.jigokusaru.lootfoundry.ui.screen;

import com.google.common.collect.Lists;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.network.chat.Component;
import net.minecraft.util.Mth;
import org.lwjgl.glfw.GLFW;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

public class AutoCompleteEditBox extends EditBox {

    private final Font font;
    private final Function<String, List<String>> suggestionProvider;
    private List<String> suggestions = Lists.newArrayList();
    private Consumer<String> valueListener;
    private boolean isSuggestionBoxOpen = false;
    private int selectedSuggestion = -1;
    private int scrollOffset = 0;
    private final int maxSuggestionsInView = 5;
    private int lastRenderedSuggestionCount = 0;

    public AutoCompleteEditBox(Font font, int x, int y, int width, int height, Component message, Function<String, List<String>> suggestionProvider) {
        super(font, x, y, width, height, message);
        this.font = font;
        this.suggestionProvider = suggestionProvider;
        super.setResponder(s -> {
            this.updateSuggestions();
            if (this.valueListener != null) {
                this.valueListener.accept(s);
            }
        });
        this.updateSuggestions();
    }

    public void setValueListener(Consumer<String> listener) {
        this.valueListener = listener;
    }

    private void updateSuggestions() {
        this.suggestions = this.suggestionProvider.apply(this.getValue());
        this.isSuggestionBoxOpen = !this.suggestions.isEmpty() && this.isFocused();
        this.selectedSuggestion = -1;
        this.scrollOffset = 0;
    }

    @Override
    public void setFocused(boolean focused) {
        super.setFocused(focused);
        if (!focused) {
            this.isSuggestionBoxOpen = false;
        }
    }

    @Override
    public void renderWidget(GuiGraphics guiGraphics, int mouseX, int mouseY, float partialTick) {
        super.renderWidget(guiGraphics, mouseX, mouseY, partialTick);
    }

    public void renderSuggestions(GuiGraphics guiGraphics, int availableHeight) {
        if (this.isSuggestionBoxOpen) {
            int boxX = this.getX();
            int boxY = this.getY() + this.height + 2;
            int boxWidth = this.getWidth();

            if (this.suggestions.isEmpty()) {
                this.lastRenderedSuggestionCount = 0;
                return;
            }

            int singleEntryHeight = this.font.lineHeight + 2;
            int maxPossibleInView = Math.max(0, availableHeight / singleEntryHeight);
            int suggestionCount = Math.min(this.suggestions.size(), Math.min(this.maxSuggestionsInView, maxPossibleInView));
            this.lastRenderedSuggestionCount = suggestionCount;

            if (suggestionCount <= 0) {
                return;
            }

            int boxHeight = suggestionCount * singleEntryHeight;
            int backgroundColor = 0xFF101010;
            int borderColor = 0xFF909090;

            guiGraphics.fill(boxX - 1, boxY - 1, boxX + boxWidth + 1, boxY + boxHeight + 1, borderColor);
            guiGraphics.fill(boxX, boxY, boxX + boxWidth, boxY + boxHeight, backgroundColor);

            for (int i = 0; i < suggestionCount; i++) {
                int suggestionIndex = i + this.scrollOffset;
                if (suggestionIndex < this.suggestions.size()) {
                    String suggestion = this.suggestions.get(suggestionIndex);
                    int entryY = boxY + i * singleEntryHeight;
                    int color = (suggestionIndex == this.selectedSuggestion) ? 0xFFFFFF00 : 0xFFE0E0E0;
                    guiGraphics.drawString(this.font, suggestion, boxX + 2, entryY + 2, color);
                }
            }
        } else {
            this.lastRenderedSuggestionCount = 0;
        }


    }

    @Override
    public boolean keyPressed(int keyCode, int scanCode, int modifiers) {
        if (this.isSuggestionBoxOpen) {
            if (keyCode == GLFW.GLFW_KEY_DOWN) {
                this.selectedSuggestion = Mth.clamp(this.selectedSuggestion + 1, -1, this.suggestions.size() - 1);
                ensureVisible(this.selectedSuggestion);
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_UP) {
                this.selectedSuggestion = Mth.clamp(this.selectedSuggestion - 1, -1, this.suggestions.size() - 1);
                ensureVisible(this.selectedSuggestion);
                return true;
            } else if (keyCode == GLFW.GLFW_KEY_ENTER || keyCode == GLFW.GLFW_KEY_TAB) {
                if (this.selectedSuggestion != -1) {
                    this.setValue(this.suggestions.get(this.selectedSuggestion));
                    this.isSuggestionBoxOpen = false;
                    return true;
                }
            } else if (keyCode == GLFW.GLFW_KEY_ESCAPE) {
                this.isSuggestionBoxOpen = false;
                return true;
            }
        }
        return super.keyPressed(keyCode, scanCode, modifiers);
    }

    @Override
    public boolean mouseClicked(double mouseX, double mouseY, int button) {
        if (this.isSuggestionBoxOpen && this.lastRenderedSuggestionCount > 0) {
            int boxX = this.getX();
            int boxY = this.getY() + this.height + 2;
            int boxWidth = this.getWidth();
            int boxHeight = this.lastRenderedSuggestionCount * (this.font.lineHeight + 2);

            if (mouseX >= boxX && mouseX < boxX + boxWidth && mouseY >= boxY && mouseY < boxY + boxHeight) {
                int clickedIndex = (int) ((mouseY - boxY) / (this.font.lineHeight + 2)) + this.scrollOffset;
                if (clickedIndex >= 0 && clickedIndex < this.suggestions.size()) {
                    this.setValue(this.suggestions.get(clickedIndex));
                    this.isSuggestionBoxOpen = false;
                }
                return true;
            }
        }

        if (!isMouseOver(mouseX, mouseY)) {
            this.isSuggestionBoxOpen = false;
        }

        return super.mouseClicked(mouseX, mouseY, button);
    }

    @Override
    public boolean mouseScrolled(double mouseX, double mouseY, double scrollX, double scrollY) {
        if (this.isSuggestionBoxOpen) {
            if (scrollY < 0) {
                this.scrollOffset = Mth.clamp(this.scrollOffset + 1, 0, Math.max(0, this.suggestions.size() - this.maxSuggestionsInView));
            } else if (scrollY > 0) {
                this.scrollOffset = Mth.clamp(this.scrollOffset - 1, 0, Math.max(0, this.suggestions.size() - this.maxSuggestionsInView));
            }
            return true;
        }
        return false;
    }

    private void ensureVisible(int index) {
        if (index == -1) return;
        if (index < this.scrollOffset) {
            this.scrollOffset = index;
        } else if (index >= this.scrollOffset + this.maxSuggestionsInView) {
            this.scrollOffset = index - this.maxSuggestionsInView + 1;
        }
    }
}