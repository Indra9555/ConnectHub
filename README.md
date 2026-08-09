# 📱 ConnectHub

# A Modern Social Media & Real-Time Chat Application for Android

Built with **Java**, **Firebase**, **Cloudinary**, and **Material Design 3**

---

# ⭐ Enjoying ConnectHub?

If you like this project, consider giving it a **⭐ Star** on GitHub.

It helps the project reach more developers and motivates future development.

---

# 📑 Quick Navigation

* [✨ Overview](#-overview)
* [🎥 Demo](#-demo)
* [📸 Screenshots](#-screenshots)
* [🚀 Features](#-features)
* [🛠 Tech Stack](#-tech-stack)
* [📂 Project Structure](#-project-structure)
* [🏗 Architecture](#-architecture)
* [✅ Implemented Features](#-implemented-features)
* [🚧 Upcoming Features](#-upcoming-features)
* [⚙️ Installation](#️-installation)
* [📖 Learning Outcomes](#-learning-outcomes)
* [🤝 Contributing](#-contributing)
* [👨‍💻 Developer](#-developer)

---

# ✨ Overview

ConnectHub is a modern Android social networking application designed to provide a complete social media experience with **real-time messaging**, **post sharing**, and **interactive communication**.

The project follows modern Android development practices while using Firebase for backend services and Cloudinary for media storage.

## Highlights

* 📱 Native Android Application
* 💬 Real-Time One-to-One & Group Chat
* 🎤 Voice Messaging
* 📷 Image Sharing
* ❤️ Emoji Reactions
* 👤 User Profiles
* ☁ Firebase Backend
* 🌐 Cloudinary Media Storage
* ⚡ Real-Time Communication
* 🏗 Repository-Based Architecture

---

# 🎥 Demo

*Add demo video/GIF here.*

---

# 📸 Screenshots

| Login | Home Feed | Profile |
| :---: | :-------: | :-----: |
|       |           |         |

| Chat | Group Chat | Comments |
| :--: | :--------: | :------: |
|      |            |          |

---

# 🚀 Features

## 👤 Authentication

* Firebase Authentication
* Secure Registration
* Login
* Auto Login
* Secure Logout

---

## 🏠 Home Feed

* Create Posts
* Image Posts
* Like Posts
* Comment System
* Real-Time Feed Updates
* Search Users

---

## 👤 User Profile

* Edit Profile
* Upload Profile Picture
* Bio
* User Information
* User Posts

---

# 💬 Real-Time Chat

## 👥 Private & Group Messaging

* One-to-One Chat
* Group Chat
* Real-Time Messaging
* Text Messages
* Image Messages
* Voice Messages
* Message Replies
* Message Reactions

---

## ✓ Message Delivery & Read States

Messages support real-time delivery and read states.

### Private Chat

* ✓ **Sent**
* ✓✓ **Seen**
* 🔵 **Blue double tick** when the message has been seen

### Group Chat

Group messages track delivery and read status for individual members.

* ✓ **Sent**
* ✓✓ **Delivered**
* 🔵✓✓ **Seen by all required members**
* Individual member delivery/read tracking using Firestore maps

The group message system tracks:

```text
deliveredTo
seenBy
```

This allows message state to update dynamically as group members receive and read messages.

---

## 👁 Read Receipts

* Messages are marked as delivered when received by another member.
* Messages currently visible on screen are marked as seen.
* Read state updates in real time through Firestore listeners.
* Group messages maintain individual `seenBy` states.
* Sender's own message is automatically considered delivered/seen by the sender.

---

## ⌨️ Typing & Presence

* Typing Indicator
* Online / Offline Status
* Last Seen

---

## 🎤 Voice Messages

* Hold to Record
* Slide to Cancel
* Recording Timer
* Waveform Visualization
* Seekable Playback
* Pause / Resume
* Playback Speed

  * 1×
  * 1.5×
  * 2×

---

## 📷 Media Messaging

* Image Messages
* Cloudinary Image Upload
* Full Screen Image Viewer
* Glide Image Loading
* Upload State Handling

---

## 💬 Interactive Messaging

* Swipe to Reply
* Reply Preview
* Emoji Reactions
* Double Tap ❤️ Reaction
* Long Press Context Menu
* Copy Message
* Delete for Everyone
* Delete for Me
* Message Search-ready architecture
* Smooth RecyclerView Animations

---

# ❤️ Social Features

* Like System
* Comment System
* Follow Users
* Notifications
* User Search

---

# 🛠 Tech Stack

| Technology                  | Purpose             |
| :-------------------------- | :------------------ |
| **Java**                    | Android Development |
| **Firebase Authentication** | User Authentication |
| **Cloud Firestore**         | Real-Time Database  |
| **Cloudinary**              | Media Storage       |
| **Glide**                   | Image Loading       |
| **RecyclerView**            | Dynamic Lists       |
| **Material Design 3**       | UI Components       |
| **MediaRecorder**           | Voice Recording     |
| **MediaPlayer**             | Voice Playback      |
| **WaveformSeekBar**         | Voice Waveform      |

---

# 📂 Project Structure

```text
app
│
├── activities
├── adapters
├── firebase
├── helpers
├── listeners
├── models
├── network
├── repository
├── services
└── utils
```

---

# 🏗 Architecture

```text
                 Android Activities
                         │
                         ▼
                  Repository Layer
                         │
          ┌──────────────┼──────────────┐
          ▼              ▼              ▼
   Authentication   Cloud Firestore   Cloudinary
          │              │              │
          └──────────────┴──────────────┘
                    Real-Time Backend
```

---

# ✅ Implemented Features

### 🔐 Authentication

* ✅ Firebase Authentication
* ✅ User Registration
* ✅ Login
* ✅ Auto Login
* ✅ Logout

### 👤 Profiles

* ✅ User Profiles
* ✅ Edit Profile
* ✅ Profile Picture
* ✅ Bio
* ✅ User Information

### 🏠 Social Feed

* ✅ Create Posts
* ✅ Image Posts
* ✅ Like System
* ✅ Comment System
* ✅ Search Users
* ✅ Real-Time Feed Updates

### 💬 Messaging

* ✅ Chat List
* ✅ One-to-One Chat
* ✅ Group Chat
* ✅ Real-Time Messaging
* ✅ Text Messages
* ✅ Image Messages
* ✅ Voice Messages
* ✅ Message Delivery State
* ✅ Message Read State
* ✅ Private Chat Read Receipts
* ✅ Group Chat Read Receipts
* ✅ Per-Member Group Delivery Tracking
* ✅ Per-Member Group Seen Tracking
* ✅ Typing Indicator
* ✅ Online Status
* ✅ Last Seen

### 🎤 Voice Messaging

* ✅ Voice Recording
* ✅ Hold to Record
* ✅ Slide to Cancel
* ✅ Recording Timer
* ✅ Waveform Visualization
* ✅ Seekable Playback
* ✅ Pause / Resume
* ✅ Playback Speed Control

### 💬 Message Interaction

* ✅ Swipe to Reply
* ✅ Reply Preview
* ✅ Emoji Reactions
* ✅ Double Tap ❤️ Reaction
* ✅ Long Press Context Menu
* ✅ Copy Message
* ✅ Delete for Everyone
* ✅ Delete for Me
* ✅ Full Screen Image Viewer
* ✅ Smooth Chat Animations
* ✅ RecyclerView DiffUtil Updates

### ☁ Media

* ✅ Cloudinary Image Upload
* ✅ Glide Image Loading
* ✅ Upload State Handling

---

# 🚧 Upcoming Features

* Edit Messages
* Push Notifications (FCM)
* Stories
* Video Messages
* Voice Calls
* Video Calls
* Dark Mode
* Message Search
* Starred Messages
* Chat Wallpaper
* User Blocking
* End-to-End Encryption
* Advanced Group Management
* Group Member Roles & Permissions

---

# ⚙️ Installation

Clone the repository:

```bash
git clone https://github.com/Indra9555/ConnectHub.git
```

Open the project in Android Studio.

Add your Firebase configuration:

```text
app/google-services.json
```

Configure Cloudinary credentials.

Sync Gradle.

Run the application.

---

# 📖 Learning Outcomes

This project has helped strengthen my understanding of:

* Android Development with Java
* Firebase Authentication
* Cloud Firestore
* Firestore Real-Time Listeners
* Real-Time Messaging Architecture
* Message Delivery & Read Receipts
* Group Messaging Systems
* Cloudinary Integration
* Media Recording & Playback
* RecyclerView
* DiffUtil
* Material Design 3
* Repository Pattern
* Clean Architecture
* Git & GitHub

---

# 🤝 Contributing

Contributions, ideas, and feedback are always welcome.

Feel free to fork this repository and submit a Pull Request.

---

# 📄 License

This project is developed for learning, portfolio, and educational purposes.

---

# 👨‍💻 Developer

## Indrajeet Verma

**B.Tech Computer Science Student**

Android Developer • Java • Firebase • Cloud Computing

GitHub: https://github.com/Indra9555

---

⭐ **If you found ConnectHub useful, don't forget to star the repository!** ⭐
