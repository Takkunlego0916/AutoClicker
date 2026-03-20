import pyautogui
import keyboard
import threading
import time
import tkinter as tk
from tkinter import messagebox

clicking = False

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

def on_hotkey_start():
    start_clicking()

def on_hotkey_stop():
    stop_clicking()

root = tk.Tk()
root.title("Auto Clicker")
root.geometry("300x180")

label = tk.Label(root, text="クリック間隔 (秒):")
label.pack(pady=5)

entry_interval = tk.Entry(root)
entry_interval.insert(0, "0.1")
entry_interval.pack(pady=5)

btn_start = tk.Button(root, text="開始", command=start_clicking)
btn_start.pack(pady=5)

btn_stop = tk.Button(root, text="停止", command=stop_clicking)
btn_stop.pack(pady=5)

label_hotkey = tk.Label(root, text="F6:開始 / F7:停止")
label_hotkey.pack(pady=10)

keyboard.add_hotkey("F6", on_hotkey_start)
keyboard.add_hotkey("F7", on_hotkey_stop)

root.mainloop()
