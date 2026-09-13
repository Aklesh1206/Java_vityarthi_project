from PIL import Image, ImageDraw, ImageFont

W, H = 1300, 780
img = Image.new("RGB", (W, H), "white")
draw = ImageDraw.Draw(img)

try:
    font = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 15)
    font_b = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans-Bold.ttf", 15)
    font_s = ImageFont.truetype("/usr/share/fonts/truetype/dejavu/DejaVuSans.ttf", 12)
except Exception:
    font = font_b = font_s = ImageFont.load_default()

actors = [
    ("Main", 80),
    ("TaskQueueManager", 270),
    ("TaskExecutorService", 480),
    ("Worker", 690),
    ("FileTaskLogger", 900),
    ("ReportGenerator", 1140),
]

top_y = 40
bottom_y = 740
box_h = 36

colors = {
    "Main": "#E6F1FB",
    "TaskQueueManager": "#EEEDFE",
    "TaskExecutorService": "#E1F5EE",
    "Worker": "#E1F5EE",
    "FileTaskLogger": "#FAEEDA",
    "ReportGenerator": "#FAECE7",
}

# draw actor boxes + lifelines
for name, x in actors:
    w = max(70, draw.textlength(name, font=font_b) + 20)
    left = max(5, x - w/2)
    right = min(W - 5, x + w/2)
    draw.rectangle([left, top_y, right, top_y + box_h], fill=colors[name], outline="#555555")
    draw.text((x, top_y + box_h/2), name, font=font_b, fill="#222222", anchor="mm")
    draw.line([x, top_y + box_h, x, bottom_y], fill="#999999", width=1)

def arrow(x1, y, x2, label, dashed=False, color="#333333"):
    y = int(y)
    if dashed:
        # dashed line
        step = 8
        xs = list(range(min(x1, x2), max(x1, x2), step))
        for i in range(0, len(xs) - 1, 2):
            draw.line([xs[i], y, min(xs[i+1], max(x1,x2)), y], fill=color, width=2)
    else:
        draw.line([x1, y, x2, y], fill=color, width=2)
    # arrowhead
    direction = 1 if x2 > x1 else -1
    ah = 8
    draw.polygon([
        (x2, y),
        (x2 - direction*ah, y - 5),
        (x2 - direction*ah, y + 5),
    ], fill=color)
    mid = (x1 + x2) / 2
    draw.text((mid, y - 16), label, font=font_s, fill="#222222", anchor="mm")

x = {name: pos for name, pos in actors}

y = 100
arrow(x["Main"], y, x["TaskQueueManager"], "submit(task)")
y += 50
arrow(x["TaskQueueManager"], y, x["Main"], "throws InvalidTaskException (if invalid)", dashed=True, color="#A32D2D")
y += 60
arrow(x["Main"], y, x["TaskExecutorService"], "start(queueManager, logger)")
y += 50
arrow(x["TaskExecutorService"], y, x["Worker"], "spawn worker threads (pool.submit)")
y += 55
arrow(x["Worker"], y, x["TaskQueueManager"], "takeNext()")
y += 45
arrow(x["TaskQueueManager"], y, x["Worker"], "returns highest-priority Task", dashed=True)
y += 55
arrow(x["Worker"], y, x["Worker"], "execute(task)  [simulate work]")
y += 45
arrow(x["Worker"], y, x["FileTaskLogger"], "logEvent(task, STARTED)")
y += 50
arrow(x["Worker"], y, x["FileTaskLogger"], "logEvent(task, COMPLETED/FAILED)")
y += 55
arrow(x["TaskExecutorService"], y, x["ReportGenerator"], "generate(processedTasks, duration)")
y += 50
arrow(x["ReportGenerator"], y, x["Main"], "writes taskflow-report.txt", dashed=True)

# self-call loop box for Worker.execute
draw.rectangle([x["Worker"]-90, 375, x["Worker"]+90, 420], outline="#888888")

# lifeline end markers
for name, xp in actors:
    draw.line([xp-6, bottom_y, xp+6, bottom_y], fill="#999999", width=1)

img.save("/home/claude/taskflow/diagrams/sequence_diagram.png")
print("saved")
