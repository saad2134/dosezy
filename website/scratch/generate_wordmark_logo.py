import os
from PIL import Image, ImageDraw, ImageFont

def main():
    # Canvas Dimensions for High-Res Brand Wordmark & Logo
    W, H = 2000, 600
    
    # 1. Transparent Canvas
    canvas = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    
    # 2. Load Squircle Icon
    icon_path = os.path.abspath("public/icon-squircle-1000px.png")
    if not os.path.exists(icon_path):
        icon_path = os.path.abspath("public/ICON.jpg")
        
    icon_size = 360
    icon_x = 120
    icon_y = (H - icon_size) // 2
    icon_center_y = icon_y + icon_size // 2

    if os.path.exists(icon_path):
        icon_img = Image.open(icon_path).convert("RGBA")
        icon_img = icon_img.resize((icon_size, icon_size), Image.Resampling.LANCZOS)
        canvas.paste(icon_img, (icon_x, icon_y), icon_img)

    # 3. Load Fonts for "Dosezy." Text
    try:
        font_title = ImageFont.truetype("C:/Windows/Fonts/bahnschrift.ttf", 220)
    except Exception:
        try:
            font_title = ImageFont.truetype("C:/Windows/Fonts/segoeuib.ttf", 200)
        except Exception:
            font_title = ImageFont.load_default()

    # 4. Draw Wordmark Text
    text_x = icon_x + icon_size + 70
    txt_draw = ImageDraw.Draw(canvas)
    
    t_box = txt_draw.textbbox((0, 0), "Dosezy", font=font_title)
    text_h = t_box[3] - t_box[1]
    text_center_offset = (t_box[1] + t_box[3]) // 2
    # Move text 10% down from center (shifting 10% up from previous 20%)
    shift_down = int(text_h * 0.10)
    title_y = (icon_center_y - text_center_offset) + shift_down

    # "Dosezy" in crisp white
    txt_draw.text((text_x, title_y), "Dosezy", font=font_title, fill=(255, 255, 255, 255))
    actual_t_box = txt_draw.textbbox((text_x, title_y), "Dosezy", font=font_title)
    
    # Sky Blue Dot "."
    dot_x = actual_t_box[2] + 4
    txt_draw.text((dot_x, title_y), ".", font=font_title, fill=(56, 189, 248, 255))

    # 5. Save Output PNG
    out_dir = "public"
    os.makedirs(out_dir, exist_ok=True)
    out_path = os.path.join(out_dir, "dosezy-wordmark-brand-logo.png")
    
    canvas.save(out_path, "PNG")
    print(f"Successfully generated Full Wordmark & Brand Logo: {out_path}")

if __name__ == "__main__":
    main()

