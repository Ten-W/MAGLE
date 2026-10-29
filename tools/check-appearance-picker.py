"""Check the two shared preset-color dialogs without saving any appearance changes.
Run with MAGLE settings open on the tablet. 
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET
from pathlib import Path
import tempfile

adb, serial = sys.argv[1:3]

def call(*args):
    return subprocess.check_output([adb, '-s', serial, *args])

def tree():
    call('shell', 'uiautomator', 'dump', '/sdcard/magle-color-check.xml')
    return ET.fromstring(call('exec-out', 'cat', '/sdcard/magle-color-check.xml'))

def find(root, key, text):
    return next(n for n in root.iter('node') if n.get(key) == text)

def tap(node):
    x1, y1, x2, y2 = map(int, re.findall(r'\d+', node.get('bounds')))
    call('shell', 'input', 'tap', str((x1+x2)//2), str((y1+y2)//2))

for target in ('主题色', '强调色'):
    root = tree()
    find(root, 'text', '颜色')
    first = find(root, 'content-desc', '选择主题色')
    second = find(root, 'content-desc', '选择强调色')
    assert re.findall(r'\d+', first.get('bounds'))[1] == re.findall(r'\d+', second.get('bounds'))[1]
    tap(find(root, 'content-desc', '选择'+target))
    root = tree()
    find(root, 'text', '选择'+target)
    for name in ('纯黑', '纯白', '中性灰', '红色', '橙色', '黄色', '绿色', '青色', '蓝色', '靛蓝', '紫色', '粉色'):
        find(root, 'text', name)
    for name in ('纯黑', '纯白', '红色'):
        tap(find(tree(), 'text', name))
        root = tree()
        node = find(root, 'text', name)
        parents = {child: parent for parent in root.iter() for child in parent}
        while node.get('selected') != 'true' and node.get('checked') != 'true' and node in parents:
            node = parents[node]
        assert node.get('selected') == 'true' or node.get('checked') == 'true', name+' did not become selected'
    screenshot = call('exec-out', 'screencap', '-p')
    (Path(tempfile.gettempdir()) / ('magle-presets-theme.png' if target == '主题色' else 'magle-presets-accent.png')).write_bytes(screenshot)
    tap(find(tree(), 'text', '取消'))
    print('PASS: '+target+', shared row, twelve presets including black, white and red, cancel without saving', flush=True)



