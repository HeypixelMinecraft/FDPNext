/*
 * FDPNext Hacked Client
 * A Super Skid Hacked Client by FDP 5.3.5.
 * https://github.com/HeypixelMinecraft/FDPNext
 */

package net.ccbluex.liquidbounce.font;

import net.minecraft.client.renderer.texture.DynamicTexture;
import org.lwjgl.opengl.GL11;

import java.awt.*;
import java.awt.geom.Rectangle2D;
import java.awt.image.BufferedImage;
import java.util.HashMap;
import java.util.Map;

public class CFont {
    protected Font font;
    protected boolean antiAlias;
    protected boolean fractionalMetrics;
    protected DynamicTexture tex;
    private final float imgSize = 512.0f;
    protected final CharData[] charData = new CharData[256];
    protected final Map<Character, DynamicCharData> dynamicCharData = new HashMap<>();
    private Font[] fallbackFonts;
    protected int fontHeight = -1;
    protected final int charOffset = 0;

    public CFont(Font font, boolean antiAlias, boolean fractionalMetrics) {
        this.font = font;
        this.antiAlias = antiAlias;
        this.fractionalMetrics = fractionalMetrics;
        this.tex = setupTexture(font, antiAlias, fractionalMetrics, this.charData);
    }

    protected DynamicTexture setupTexture(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        try {
            return new DynamicTexture(generateFontImage(font, antiAlias, fractionalMetrics, chars));
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    protected BufferedImage generateFontImage(Font font, boolean antiAlias, boolean fractionalMetrics, CharData[] chars) {
        BufferedImage bufferedImage = new BufferedImage(512, 512, 2);
        Graphics2D g = (Graphics2D) bufferedImage.getGraphics();
        g.setFont(font);
        g.setColor(new Color(255, 255, 255, 0));
        g.fillRect(0, 0, 512, 512);
        g.setColor(Color.WHITE);
        g.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        g.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        g.setRenderingHint(RenderingHints.KEY_ANTIALIASING, antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        FontMetrics fontMetrics = g.getFontMetrics();
        int charHeight = 0;
        int positionX = 0;
        int positionY = 1;
        for (int i = 0; i < chars.length; i++) {
            char ch = (char) i;
            CharData charData = new CharData();
            Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(ch), g);
            charData.width = dimensions.getBounds().width + 8;
            charData.height = dimensions.getBounds().height;
            if (positionX + charData.width >= 512) {
                positionX = 0;
                positionY += charHeight;
                charHeight = 0;
            }
            if (charData.height > charHeight) {
                charHeight = charData.height;
            }
            charData.storedX = positionX;
            charData.storedY = positionY;
            if (charData.height > this.fontHeight) {
                this.fontHeight = charData.height;
            }
            chars[i] = charData;
            g.drawString(String.valueOf(ch), positionX + 2, positionY + fontMetrics.getAscent());
            positionX += charData.width;
        }
        return bufferedImage;
    }

    public void drawChar(CharData[] chars, char c, float x, float y) throws ArrayIndexOutOfBoundsException {
        try {
            if (c < chars.length) {
                drawQuad(x, y, (float) chars[c].width, (float) chars[c].height, (float) chars[c].storedX, (float) chars[c].storedY, (float) chars[c].width, (float) chars[c].height);
            } else {
                drawDynamicChar(c, x, y);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    protected void drawDynamicChar(char c, float x, float y) {
        DynamicCharData charData = getDynamicCharData(c);
        GL11.glBindTexture(3553, charData.texture.getGlTextureId());
        drawDynamicQuad(x, y, charData.width, charData.height);
    }

    protected void drawQuad(float x, float y, float width, float height, float srcX, float srcY, float srcWidth, float srcHeight) {
        float renderSRCX = srcX / 512.0f;
        float renderSRCY = srcY / 512.0f;
        float renderSRCWidth = srcWidth / 512.0f;
        float renderSRCHeight = srcHeight / 512.0f;
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY + renderSRCHeight);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2f(renderSRCX + renderSRCWidth, renderSRCY);
        GL11.glVertex2d(x + width, y);
    }

    protected void drawDynamicQuad(float x, float y, float width, float height) {
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2d(x + width, y);
        GL11.glTexCoord2f(0.0f, 0.0f);
        GL11.glVertex2d(x, y);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(0.0f, 1.0f);
        GL11.glVertex2d(x, y + height);
        GL11.glTexCoord2f(1.0f, 1.0f);
        GL11.glVertex2d(x + width, y + height);
        GL11.glTexCoord2f(1.0f, 0.0f);
        GL11.glVertex2d(x + width, y);
    }

    protected DynamicCharData getDynamicCharData(char c) {
        DynamicCharData cached = this.dynamicCharData.get(c);
        if (cached != null) {
            return cached;
        }

        DynamicCharData charData = new DynamicCharData();
        BufferedImage charImage = generateDynamicCharImage(c, charData);
        charData.texture = new DynamicTexture(charImage);
        this.dynamicCharData.put(c, charData);
        return charData;
    }

    protected BufferedImage generateDynamicCharImage(char c, DynamicCharData charData) {
        Font renderFont = getFontForChar(c);
        BufferedImage tempImage = new BufferedImage(1, 1, BufferedImage.TYPE_INT_ARGB);
        Graphics2D tempGraphics = (Graphics2D) tempImage.getGraphics();
        tempGraphics.setFont(renderFont);
        tempGraphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, this.fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        tempGraphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        tempGraphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);

        FontMetrics fontMetrics = tempGraphics.getFontMetrics();
        Rectangle2D dimensions = fontMetrics.getStringBounds(String.valueOf(c), tempGraphics);
        charData.width = Math.max(dimensions.getBounds().width + 8, 7);
        charData.height = Math.max(dimensions.getBounds().height + 3, renderFont.getSize());

        BufferedImage charImage = new BufferedImage(charData.width, charData.height, BufferedImage.TYPE_INT_ARGB);
        Graphics2D graphics = (Graphics2D) charImage.getGraphics();
        graphics.setFont(renderFont);
        graphics.setColor(Color.WHITE);
        graphics.setRenderingHint(RenderingHints.KEY_FRACTIONALMETRICS, this.fractionalMetrics ? RenderingHints.VALUE_FRACTIONALMETRICS_ON : RenderingHints.VALUE_FRACTIONALMETRICS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_TEXT_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_TEXT_ANTIALIAS_ON : RenderingHints.VALUE_TEXT_ANTIALIAS_OFF);
        graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, this.antiAlias ? RenderingHints.VALUE_ANTIALIAS_ON : RenderingHints.VALUE_ANTIALIAS_OFF);
        graphics.drawString(String.valueOf(c), 3, fontMetrics.getAscent() + 1);
        return charImage;
    }

    protected Font getFontForChar(char c) {
        if (this.font.canDisplay(c)) {
            return this.font;
        }

        for (Font fallbackFont : getFallbackFonts()) {
            if (fallbackFont.canDisplay(c)) {
                return fallbackFont;
            }
        }

        return this.font;
    }

    private Font[] getFallbackFonts() {
        if (this.fallbackFonts != null) {
            return this.fallbackFonts;
        }

        String[] preferredFontNames = new String[] {"Microsoft YaHei UI", "Microsoft YaHei", "SimSun", "NSimSun", "Dialog", "SansSerif"};
        GraphicsEnvironment graphicsEnvironment = GraphicsEnvironment.getLocalGraphicsEnvironment();
        Font[] allFonts = graphicsEnvironment.getAllFonts();
        Map<String, Font> selectedFonts = new HashMap<>();

        for (String preferredFontName : preferredFontNames) {
            for (Font availableFont : allFonts) {
                if (availableFont.getFontName().equals(preferredFontName) || availableFont.getFamily().equals(preferredFontName)) {
                    selectedFonts.put(preferredFontName, availableFont.deriveFont(this.font.getStyle(), (float) this.font.getSize()));
                    break;
                }
            }
        }

        if (selectedFonts.isEmpty()) {
            this.fallbackFonts = new Font[] {new Font("Dialog", this.font.getStyle(), this.font.getSize())};
        } else {
            this.fallbackFonts = selectedFonts.values().toArray(new Font[0]);
        }

        return this.fallbackFonts;
    }

    public int getStringHeight(String text) {
        return getHeight();
    }

    public int getHeight() {
        return (this.fontHeight - 8) / 2;
    }

    public int getStringWidth(String text) {
        int width = 0;
        char[] arrc = text.toCharArray();
        for (char c : arrc) {
            if (c < this.charData.length) {
                width += (this.charData[c].width - 8) + this.charOffset;
            } else {
                width += (getDynamicCharData(c).width - 8) + this.charOffset;
            }
        }
        return width / 2;
    }

    public boolean isAntiAlias() {
        return this.antiAlias;
    }

    public void setAntiAlias(boolean antiAlias) {
        if (this.antiAlias != antiAlias) {
            this.antiAlias = antiAlias;
            this.dynamicCharData.clear();
            this.tex = setupTexture(this.font, antiAlias, this.fractionalMetrics, this.charData);
        }
    }

    public boolean isFractionalMetrics() {
        return this.fractionalMetrics;
    }

    public void setFractionalMetrics(boolean fractionalMetrics) {
        if (this.fractionalMetrics != fractionalMetrics) {
            this.fractionalMetrics = fractionalMetrics;
            this.dynamicCharData.clear();
            this.tex = setupTexture(this.font, this.antiAlias, fractionalMetrics, this.charData);
        }
    }

    public Font getFont() {
        return this.font;
    }

    public void setFont(Font font) {
        this.font = font;
        this.dynamicCharData.clear();
        this.fallbackFonts = null;
        this.tex = setupTexture(font, this.antiAlias, this.fractionalMetrics, this.charData);
    }

    /* loaded from: LiquidBounce-b73.jar:net/ccbluex/liquidbounce/CFont$CharData.class */
    protected static class CharData {
        public int width;
        public int height;
        public int storedX;
        public int storedY;

        protected CharData() {
        }
    }

    protected static class DynamicCharData {
        public int width;
        public int height;
        public DynamicTexture texture;
    }
}
