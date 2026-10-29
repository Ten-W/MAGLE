"""Read-only tablet UI check: nested foldouts, scrolled collapse and unpin.

Run with the directory pinned and the library loaded. Requires Pillow.
Pass --slow-motion only when inspecting captured intermediate frames.
"""
import io
import re
import subprocess
import sys
import tempfile
import time
import xml.etree.ElementTree as ET
from pathlib import Path
from PIL import Image

adb, serial = sys.argv[1:3]
out = Path(tempfile.gettempdir()) / 'magle-directory-check'
out.mkdir(exist_ok=True)

def call(*args):
    return subprocess.check_output([adb, '-s', serial, *args])

def tree():
    call('shell', 'uiautomator', 'dump', '/sdcard/magle-directory-check.xml')
    return ET.fromstring(call('exec-out', 'cat', '/sdcard/magle-directory-check.xml'))

def find(root, **attrs):
    return next(n for n in root.iter('node') if all(n.get(k) == v for k, v in attrs.items()))

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    call('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

def sample(label):
    frames = []
    for i in range(4):
        frame = Image.open(io.BytesIO(call('exec-out', 'screencap', '-p')))
        frame.save(out / f'{label}-{i}.png')
        frames.append(frame.crop((27, 800, 747, 1750)).resize((240, 317)))
        time.sleep(.09)
    sheet = Image.new('RGB', (960, 317))
    for i, frame in enumerate(frames):
        sheet.paste(frame, (i*240, 0))
    sheet.save(out / f'{label}.png')
    time.sleep(1)

old = call('shell', 'settings', 'get', 'global', 'animator_duration_scale').decode().strip()
slow = '--slow-motion' in sys.argv[3:]
try:
    if slow:
        call('shell', 'settings', 'put', 'global', 'animator_duration_scale', '5')
    root = tree()
    if any(n.get('content-desc') == '展开文件夹' for n in root.iter('node')):
        tap(find(root, **{'content-desc': '展开文件夹'}))
        time.sleep(1)
    root = tree()
    label = find(root, text='TenW')
    parents = {child: parent for parent in root.iter() for child in parent}
    row = label
    while not any(n.get('content-desc') in ('展开', '收起') for n in row.iter('node')):
        row = parents[row]
    arrow = next(n for n in row.iter('node') if n.get('content-desc') in ('展开', '收起'))
    if arrow.get('content-desc') == '收起':
        tap(arrow)
        time.sleep(1)
    tap(arrow)
    sample('nested-open')
    root = tree()
    tap(find(root, **{'content-desc': '收起'}))
    sample('nested-close')
    call('shell', 'input', 'swipe', '300', '1550', '300', '1300', '400')
    root = tree()
    tap(find(root, **{'content-desc': '收起文件夹'}))
    sample('scrolled-close')
    root = tree()
    find(root, **{'content-desc': '展开文件夹'})
    assert not any(n.get('text') == 'TenW' for n in root.iter('node'))
    tap(find(root, **{'content-desc': '取消固定目录'}))
    time.sleep(2)
    root = tree()
    assert not any(n.get('content-desc') in ('固定目录', '取消固定目录', '展开文件夹') for n in root.iter('node'))
    print(f'PASS: nested open/close, scrolled collapse, unpin closes; frames: {out}')
finally:
    if slow:
        # Deleting an unset key alone can leave the framework's cached scale at 5x.
        call('shell', 'settings', 'put', 'global', 'animator_duration_scale', '1.0' if old == 'null' else old)
        if old == 'null':
            call('shell', 'settings', 'delete', 'global', 'animator_duration_scale')
