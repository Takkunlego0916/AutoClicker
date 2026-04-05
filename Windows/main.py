import pyautogui
import keyboard
import threading
import time
import tkinter as tk
from tkinter import messagebox

clicking = False
hotkey_start = "f6"
hotkey_stop = "f7"
start_hotkey_handle = None
stop_hotkey_handle = None


def normalize_key(keysym: str) -> str:
    """Tkinterのkeysymをkeyboard用の名前に寄せる"""
    keymap = {
        "Escape": "esc",
        "Return": "enter",
        "space": "space",
        "BackSpace": "backspace",
        "Tab": "tab",
        "Delete": "delete",
        "Insert": "insert",
        "Home": "home",
        "End": "end",
        "Prior": "page up",
        "Next": "page down",
        "Up": "up",
        "Down": "down",
        "Left": "left",
        "Right": "right",
    }

    if keysym in keymap:
        return keymap[keysym]

    if len(keysym) == 1:
        return keysym.lower()

    return keysym.lower()


def click_loop(interval):
    global clicking
    while clicking:
        pyautogui.click()
        time.sleep(interval)


def start_clicking():
    global clicking
    if clicking:
        return

    try:
        interval = float(entry_interval.get())
        if interval <= 0:
            raise ValueError
    except:
        messagebox.showerror("エラー", "正しいクリック間隔を入力してください")
        return

    clicking = True
    thread = threading.Thread(target=click_loop, args=(interval,), daemon=True)
    thread.start()


def stop_clicking():
    global clicking
    clicking = False


def update_hotkey_label():
    label_hotkey.config(
        text=f"開始: {hotkey_start.upper()} / 停止: {hotkey_stop.upper()}"
    )


def unregister_hotkeys():
    global start_hotkey_handle, stop_hotkey_handle

    if start_hotkey_handle is not None:
        try:
            keyboard.remove_hotkey(start_hotkey_handle)
        except:
            pass
        start_hotkey_handle = None

    if stop_hotkey_handle is not None:
        try:
            keyboard.remove_hotkey(stop_hotkey_handle)
        except:
            pass
        stop_hotkey_handle = None


def register_hotkeys():
    global start_hotkey_handle, stop_hotkey_handle

    unregister_hotkeys()

    start_hotkey_handle = keyboard.add_hotkey(hotkey_start, start_clicking)
    stop_hotkey_handle = keyboard.add_hotkey(hotkey_stop, stop_clicking)

    update_hotkey_label()


def capture_key(parent, title):
    """次に押した1キーを取得する"""
    result = {"key": None}

    win = tk.Toplevel(parent)
    win.title(title)
    win.geometry("320x140")
    win.resizable(False, False)
    win.transient(parent)
    win.grab_set()

    label = tk.Label(win, text="割り当てたいキーを1つ押してください")
    label.pack(pady=20)

    current = tk.Label(win, text="待機中...")
    current.pack(pady=5)

    def on_key(event):
        key = normalize_key(event.keysym)

        # 修飾キー単体は無視
        if key in ("shift_l", "shift_r", "control_l", "control_r", "alt_l", "alt_r"):
            return

        result["key"] = key
        win.destroy()

    def cancel():
        win.destroy()

    btn_cancel = tk.Button(win, text="キャンセル", command=cancel)
    btn_cancel.pack(pady=10)

    win.bind("<KeyPress>", on_key)
    win.focus_force()
    win.wait_window()

    return result["key"]


def open_settings():
    settings = tk.Toplevel(root)
    settings.title("設定")
    settings.geometry("360x220")
    settings.resizable(False, False)
    settings.transient(root)
    settings.grab_set()

    lbl_info = tk.Label(settings, text="ホットキー設定")
    lbl_info.pack(pady=10)

    lbl_current = tk.Label(
        settings,
        text=f"現在の設定\n開始: {hotkey_start.upper()}\n停止: {hotkey_stop.upper()}",
        justify="left"
    )
    lbl_current.pack(pady=5)

    def change_start_key():
        nonlocal lbl_current
        global hotkey_start
        key = capture_key(settings, "開始キーを設定")
        if key:
            hotkey_start = key
            register_hotkeys()
            lbl_current.config(
                text=f"現在の設定\n開始: {hotkey_start.upper()}\n停止: {hotkey_stop.upper()}"
            )

    def change_stop_key():
        nonlocal lbl_current
        global hotkey_stop
        key = capture_key(settings, "停止キーを設定")
        if key:
            hotkey_stop = key
            register_hotkeys()
            lbl_current.config(
                text=f"現在の設定\n開始: {hotkey_start.upper()}\n停止: {hotkey_stop.upper()}"
            )

    btn_start = tk.Button(settings, text="開始キーを変更", command=change_start_key)
    btn_start.pack(pady=5)

    btn_stop = tk.Button(settings, text="停止キーを変更", command=change_stop_key)
    btn_stop.pack(pady=5)

    btn_close = tk.Button(settings, text="閉じる", command=settings.destroy)
    btn_close.pack(pady=15)


def on_close():
    unregister_hotkeys()
    root.destroy()


root = tk.Tk()
root.title("Auto Clicker")
root.geometry("300x220")
root.protocol("WM_DELETE_WINDOW", on_close)

label = tk.Label(root, text="クリック間隔 (秒):")
label.pack(pady=5)

entry_interval = tk.Entry(root)
entry_interval.insert(0, "0.1")
entry_interval.pack(pady=5)

btn_start = tk.Button(root, text="開始", command=start_clicking)
btn_start.pack(pady=5)

btn_stop = tk.Button(root, text="停止", command=stop_clicking)
btn_stop.pack(pady=5)

btn_settings = tk.Button(root, text="設定", command=open_settings)
btn_settings.pack(pady=5)

label_hotkey = tk.Label(root, text="")
label_hotkey.pack(pady=10)

register_hotkeys()

root.mainloop()
