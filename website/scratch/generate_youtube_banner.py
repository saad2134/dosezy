import os
import sys
from PIL import Image, ImageDraw, ImageFont, ImageFilter

def hex_to_rgb(hex_str):
    hex_str = hex_str.lstrip('#')
    return tuple(int(hex_str[i:i+2], 16) for i in (0, 2, 4))

def main():
    # YouTube Banner Standards
    CANVAS_W, CANVAS_H = 2560, 1440
    SAFE_W, SAFE_H = 1546, 423
    SAFE_X = (CANVAS_W - SAFE_W) // 2  # 507
    SAFE_Y = (CANVAS_H - SAFE_H) // 2  # 508

    # 1. Base Canvas Creation
    canvas = Image.new("RGBA", (CANVAS_W, CANVAS_H), (3, 4, 7, 255))
    
    # Deep Obsidian Background Gradient & Ambient Glows
    glow_bg = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    g_draw = ImageDraw.Draw(glow_bg)
    
    # Large Sky Blue Center Glow behind Safe Area
    g_draw.ellipse(
        [(SAFE_X - 100, SAFE_Y - 50), (SAFE_X + SAFE_W + 100, SAFE_Y + SAFE_H + 50)],
        fill=(2, 132, 199, 45)
    )
    # Royal Blue Accent Glow on Right
    g_draw.ellipse(
        [(CANVAS_W - 800, CANVAS_H - 800), (CANVAS_W + 200, CANVAS_H + 200)],
        fill=(37, 99, 235, 35)
    )
    glow_bg = glow_bg.filter(ImageFilter.GaussianBlur(120))
    canvas = Image.alpha_composite(canvas, glow_bg)

    # 2. Draw Decorative Grid Lines in Background
    grid_draw = ImageDraw.Draw(canvas)
    grid_color = (255, 255, 255, 6)
    step = 70
    for x in range(0, CANVAS_W, step):
        grid_draw.line([(x, 0), (x, CANVAS_H)], fill=grid_color, width=1)
    for y in range(0, CANVAS_H, step):
        grid_draw.line([(0, y), (CANVAS_W, y)], fill=grid_color, width=1)

    # 3. Safe Area Glassmorphic Container Card
    card_layer = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    card_draw = ImageDraw.Draw(card_layer)
    
    # Outer Glass Card with rounded corners
    card_draw.rounded_rectangle(
        [(SAFE_X, SAFE_Y), (SAFE_X + SAFE_W, SAFE_Y + SAFE_H)],
        radius=36,
        fill=(10, 12, 16, 220),
        outline=(255, 255, 255, 45),
        width=2
    )

    # Top Highlight Accent Line
    card_draw.rounded_rectangle(
        [(SAFE_X + 2, SAFE_Y + 2), (SAFE_X + SAFE_W - 2, SAFE_Y + 6)],
        radius=4,
        fill=(56, 189, 248, 180)
    )
    canvas = Image.alpha_composite(canvas, card_layer)

    # 4. Load & Position Squircle Icon inside Safe Area
    icon_path = os.path.abspath("public/icon-squircle-1000px.png")
    if not os.path.exists(icon_path):
        icon_path = os.path.abspath("public/ICON.jpg")
    
    icon_size = 280
    icon_x = SAFE_X + 60
    icon_y = SAFE_Y + (SAFE_H - icon_size) // 2

    if os.path.exists(icon_path):
        icon_img = Image.open(icon_path).convert("RGBA")
        icon_img = icon_img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)

        # Icon Shadow & Glow
        icon_shadow = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
        is_draw = ImageDraw.Draw(icon_shadow)
        is_draw.rounded_rectangle(
            [(icon_x - 10, icon_y - 10), (icon_x + icon_size + 10, icon_y + icon_size + 10)],
            radius=44,
            fill=(2, 132, 199, 100)
        )
        icon_shadow = icon_shadow.filter(ImageFilter.GaussianBlur(25))
        canvas = Image.alpha_composite(canvas, icon_shadow)
        canvas.paste(icon_img, (icon_x, icon_y), icon_img)

    # 5. Fonts Setup
    try:
        font_title = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", 104)
        font_sub = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 26)
        font_chip = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 19)
    except Exception:
        font_title = ImageFont.load_default()
        font_sub = ImageFont.load_default()
        font_chip = ImageFont.load_default()

    # 6. Title and Motto Text
    text_x = icon_x + icon_size + 50
    title_y = SAFE_Y + 55

    txt_draw = ImageDraw.Draw(canvas)
    # Title "Dosezy."
    txt_draw.text((text_x, title_y), "Dosezy", font=font_title, fill=(255, 255, 255, 255))
    t_box = txt_draw.textbbox((text_x, title_y), "Dosezy", font=font_title)
    dot_x = t_box[2] + 4
    txt_draw.text((dot_x, title_y), ".", font=font_title, fill=(56, 189, 248, 255))

    # Motto Subtitle
    motto_y = title_y + 118
    txt_draw.text(
        (text_x, motto_y),
        "Open-Source Local-First Medication Tracker & Cloud Sync",
        font=font_sub,
        fill=(224, 242, 254, 230)
    )

    # 7. Chips / Feature Pill Boxes Row inside Safe Area
    chips = [
        ("100% Local Privacy", (56, 189, 248)),
        ("12-Hour Grid Picker", (129, 140, 248)),
        ("Caregiver Cloud", (56, 189, 248)),
        ("Docker Self-Host", (168, 85, 247)),
        ("Android & iOS", (52, 211, 153))
    ]

    chip_layer = Image.new("RGBA", (CANVAS_W, CANVAS_H), (0, 0, 0, 0))
    ch_draw = ImageDraw.Draw(chip_layer)

    curr_cx = text_x
    chip_y = motto_y + 60

    for label, col_rgb in chips:
        c_box = ch_draw.textbbox((0, 0), label, font=font_chip)
        cw = (c_box[2] - c_box[0]) + 28
        ch = 44

        # Wrap to next row if overflowing safe width
        if curr_cx + cw > SAFE_X + SAFE_W - 40:
            curr_cx = text_x
            chip_y += 56

        ch_draw.rounded_rectangle(
            [(curr_cx, chip_y), (curr_cx + cw, chip_y + ch)],
            radius=22,
            fill=(255, 255, 255, 18),
            outline=(col_rgb[0], col_rgb[1], col_rgb[2], 120),
            width=1
        )
        ch_draw.text((curr_cx + 14, chip_y + 11), label, font=font_chip, fill=(255, 255, 255, 245))
        curr_cx += cw + 14

    canvas = Image.alpha_composite(canvas, chip_layer)

    # 8. Save Final YouTube Channel Banner Output
    output_dir = "public"
    os.makedirs(output_dir, exist_ok=True)
    out_path = os.path.join(output_dir, "youtube_banner_2560x1440.png")
    
    canvas.convert("RGB").save(out_path, "PNG", quality=95)
    print(f"Successfully generated YouTube Channel Banner: 2560x1440 -> {out_path}")

if __name__ == "__main__":
    main()
