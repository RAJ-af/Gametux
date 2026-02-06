
---

🧠 ONE-LINE PRODUCT DEFINITION (LOCK THIS)

Mobile phone = wireless retro console (brain + controller)
Android TV = wireless display (screen + speaker)
Game phone pe run hota hai, TV pe dikhta hai
Ultra-low latency target: ≤13 ms (best case)


---

1️⃣ USER SIDE WORKFLOW (JO USER DEKHTA HAI)

User ka experience bilkul aisa hoga 👇

1. User TV on karta hai


2. TV pe app open karta hai

Screen pe likha hota hai:
“Waiting for Console…”



3. User phone pe app open karta hai


4. Phone automatically TV detect karta hai

User ko bas dikhe:
“Connected to TV ✅”



5. User phone se ROM import karta hai


6. ROM select karta hai


7. Game start


8. Game TV pe fullscreen chal raha


9. Phone screen = controller buttons



❌ No cable
❌ No cast button
❌ No room code
❌ No QR
❌ No settings bakchodi

Exactly console jaisa feel.


---

2️⃣ MOBILE APP (CONSOLE APP) – FULL WORKING

🔹 Mobile app ka role

Mobile app sab kuch hai:

Emulator run karta hai

ROM load karta hai

Game logic handle karta hai

Input (controller) leta hai

Video + audio banata hai

TV ko stream karta hai


👉 Mobile = actual console hardware


---

🔹 Mobile app internal workflow (STEP BY STEP)

🟢 STEP 1: App Launch

App open hote hi:

Foreground Service start hoti hai

Battery optimization disable prompt (1st time)

Network discovery start




---

🟢 STEP 2: TV Auto-Detect

Mobile app LAN pe scan karta hai

TV app ne jo service broadcast ki hoti hai usko pakadta hai

Same router hone par auto connect


Tech:

Android NSD (mDNS)


User ko kuch pata bhi nahi chalta 😌


---

🟢 STEP 3: ROM Import (MOBILE ONLY)

User “Add ROM” pe tap karta hai

ROM phone storage se select hoti hai

App ROM extension check karta hai:


Extension	Emulator

.nes	NES
.gb	Game Boy
.gbc	GBC
.gba	GBA
.nds	NDS (later)


❌ TV ko ROM ka koi access nahi
❌ Legal risk TV side pe zero


---

🟢 STEP 4: Game Start

User ROM select karta hai

App:

Correct emulator core load karta hai

Emulator thread start karta hai

Controller UI activate karta hai




---

🟢 STEP 5: BACKGROUND GAME EXECUTION (IMPORTANT)

Yeh sabse critical part hai 🔥

Emulator background / headless mode me run hota hai

Mobile screen pe game render nahi hota

Game ka output direct:

GPU texture → encoder



Mobile screen pe sirf: 👉 controller buttons


---

🟢 STEP 6: Streaming to TV

Emulator frame generate karta hai

Frame hardware encoder ko jata hai

Encoder ultra-low-latency H.264 frame banata hai

Frame WebRTC (UDP) se TV ko jata hai


No buffering. No retransmit.


---

3️⃣ ANDROID TV APP (CLIENT / DISPLAY APP)

🔹 TV app ka role

TV app dumb device hai (intentionally):

Emulator ❌

ROM ❌

Input ❌


TV sirf:

Video dikhata hai

Audio bajata hai



---

🔹 TV app workflow

🟢 STEP 1: TV App Launch

App open hote hi:

Network pe announce karta hai: “I am a console display”



Tech:

Android NSD service publish



---

🟢 STEP 2: Waiting Screen

TV pe likha hota hai:

> “Waiting for console…”



Exactly jaise PlayStation idle screen.


---

🟢 STEP 3: Phone Connects

Phone connect hota hai

TV UI change hota hai:


> “Console Connected 🎮”




---

🟢 STEP 4: Stream Receive

TV WebRTC stream receive karta hai

Hardware decoder se decode karta hai

Fullscreen game render karta hai

TV speakers se sound


Remote ka role = ZERO.


---

4️⃣ CONTROLLER SYSTEM (MVP)

🔹 Controller kaha hoga?

👉 Mobile app ke andar

Touch buttons

D-Pad

A / B

Start / Select


Input path (ZERO LAG goal):

Touch → Emulator Input → Frame

No IPC
No Bluetooth
No extra app

Isliye latency sabse kam.


---

5️⃣ TECHNOLOGY STACK (WHY + WHAT)

📱 Mobile App

Language: Kotlin

Emulator: C / C++ (NDK)

Streaming: WebRTC (LAN only)

Video: H.264 hardware encoder

Audio: PCM / AAC (low buffer)



---

📺 TV App

Language: Kotlin

UI: Leanback

Streaming: WebRTC receiver

Decode: MediaCodec (low latency mode)



---

🌐 Networking

Same router required

TV LAN + Phone Wi-Fi = OK

No internet required after connect



---

6️⃣ LATENCY STRATEGY (UNDER 13 ms TARGET)

Key rules:

No casting

No cloud

No jitter buffer

No B-frames

No Bluetooth controller

Foreground service only


Best case: 👉 10–13 ms
Worst good case: 👉 15–18 ms


---

7️⃣ FEATURES LIST (CLEAR)

✅ MVP FEATURES

Wireless only

Auto TV detect

Phone = controller

TV fullscreen gameplay

NES / GB / GBA

ROM import on phone

Background game execution



---

❌ NOT IN MVP

Multiplayer

Online play

Cloud saves

Heavy consoles

Separate controller app



---

8️⃣ FUTURE (SAFE UPGRADE PATH)

Phase 2

NDS

Save / Load

Better scaling


Phase 3

Multiplayer (same TV)

Separate controller app


Phase 4

Heavy consoles

Accounts



---

🧠 FINAL MENTAL MODEL (YAAD RAKH)

Soch aise:

> TV = monitor
Phone = PS2 box + controller
Wi-Fi = HDMI cable



Agar tu ye samajh gaya —
toh tu poora project samajh gaya 😌🔥


---

✅ QUICK SANITY CHECK (FINAL CONFIRMATION)
Tu jo soch raha hai, wo real-world me possible hai IF:
✔ Phone hi emulator + console hai
✔ TV sirf display + audio hai
✔ ROM import sirf phone pe hota hai
✔ Game background/headless mode me run hota hai
✔ Mobile screen pe sirf controller hota hai
✔ Auto-detect same network pe hota hai
✔ WebRTC ko game-stream mode me use kiya jata hai
✔ Target latency best case 10–13 ms, not promise to all users
👉 In sab me koi logical contradiction nahi.

----

⚠️ REALITY BOUNDARIES (JO YAAD RAKHNA HAI)
Bas ye 5 cheezein dimaag me lock kar le:
13 ms ek guarantee nahi, target hai
Har phone/TV pe same result nahi aayega
Cheap routers latency badha denge
Old TVs biggest bottleneck hote hain
First version me device whitelist rakhna padega
Ye accept kiya toh project smooth chalega.

Background” word ka ek clarification
Jab tum likhte ho:
Emulator background / headless mode me run hota hai
👉 Iska exact technical meaning ye hona chahiye:
Emulator foreground service me run kare
UI surface TV ke liye ho
Phone screen pe game render na ho
💡 Reason:
Android me true “background process” ko OS maar sakta hai.
Tum already ye samajh rahe ho — bas wording future docs me clear rakhna.


GPU texture → encoder line (correct, but subtle)
Tumne likha:
GPU texture → encoder
Ye conceptually correct hai 👍
Implementation me ye hoga:
Emulator frame → Surface / ImageReader
MediaCodec encoder → Surface input mode
👉 Matlab CPU copy avoid karna hai.
Design level pe tum sahi ho — code me bas careful rehna hoga.

Latency section – wording PERFECT hai
Ye part specially acha likha hai 👇
Target latency best case 10–13 ms, not promise to all users
Isse:
Product expectations clear
Legal / UX dono safe
Over-promising nahi
👏 Isko bilkul mat change karna.


