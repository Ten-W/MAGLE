"""Read-only UI check of official bottom-bar scroll behavior on a loaded tablet.

Usage: python check-bottom-navigation.py <adb.exe> <serial>
Leaves the original view mode selected; never edits library files.
"""
import re
import subprocess
import sys
import time
import xml.etree.ElementTree as ET

adb, serial = sys.argv[1:3]

def call(*args):
    return subprocess.check_output([adb, '-s', serial, *map(str, args)])

def tree():
    call('shell', 'uiautomator', 'dump', '/sdcard/magle-bottom-check.xml')
    return ET.fromstring(call('exec-out', 'cat', '/sdcard/magle-bottom-check.xml'))

def bounds(node):
    return tuple(map(int, re.findall(r'\d+', node.get('bounds'))))

def tap(node):
    x1, y1, x2, y2 = bounds(node)
    call('shell', 'input', 'tap', (x1+x2)//2, (y1+y2)//2)

def find(root, key, value):
    return next(n for n in root.iter('node') if n.get(key) == value)

def swipe(up):
    call('shell', 'input', 'swipe', width*.6, height*(.77 if up else .35),
         width*.6, height*(.35 if up else .77), 550)
    time.sleep(.5)

def gallery(root):
    names = {n.get('text') for n in root.iter('node') if n.get('text')}
    return [(n.get('content-desc'), bounds(n)) for n in root.iter('node')
            if n.get('content-desc') and (n.get('class') == 'android.widget.ImageView' or
                n.get('class') == 'android.view.View' and n.get('content-desc') in names)][:3]

def select_mode(mode):
    root = tree()
    modes = [n for n in root.iter('node') if n.get('content-desc') in labels]
    tap(modes[-1])
    root = tree()
    tap(find(root, 'content-desc', mode))
    time.sleep(.4)

root = tree()
width = max(bounds(n)[2] for n in root.iter('node'))
height = max(bounds(n)[3] for n in root.iter('node'))
labels = ('网格视图', '瀑布流', '列表视图')
original = next(n.get('content-desc') for n in root.iter('node') if n.get('content-desc') in labels)
try:
    for mode in labels:
        select_mode(mode)
        find(tree(), 'text', '视图')
        swipe(True)
        assert not any(n.get('text') == '视图' for n in tree().iter('node')), f'{mode}: did not hide'
        swipe(False)
        root = tree()
        find(root, 'text', '视图')
        anchor = gallery(root)
        tap(find(root, 'text', '设置'))
        assert not gallery(tree()), 'Gallery is accessible beneath settings'
        tap(find(tree(), 'text', '视图'))
        root = tree()
        assert anchor == gallery(root), 'Clicking View changed the gallery position'
        # Horizontal navigation must survive changes to the bottom-bar presentation.
        call('shell', 'input', 'swipe', width*.8, height*.4, width*.2, height*.4, 600)
        time.sleep(.4)
        assert not gallery(tree()), 'Left swipe did not reach settings'
        call('shell', 'input', 'swipe', width*.2, height*.4, width*.8, height*.4, 600)
        time.sleep(.4)
        assert anchor == gallery(tree()), 'Right swipe did not restore gallery position'
        tap(find(tree(), 'text', '设置'))
        call('shell', 'input', 'keyevent', 4)
        root = tree()
        assert anchor == gallery(root), f'Returning from settings changed gallery anchors: {anchor!r} -> {gallery(root)!r}'
        find(root, 'text', '视图')
        print(f'PASS: {mode}, hide/show, button navigation, both swipe directions and back preserve position', flush=True)
finally:
    select_mode(original)
