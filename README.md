# 💳 MoneyMate: Smart Finance & Payment Manager (Console Edition)

## 📌 Overview
**MoneyMate** is a console-based Java application designed to simplify personal finance management.  
It enables users to securely register, log in, and manage their accounts with features like deposits, withdrawals, transfers, transaction history, smart reminders, budget tracking, donations to charity trusts, and grocery item management.  

This project is built with **OOP principles**, **collections**, **exception handling**, **multithreading**, and **AES encryption** for password security.  
Future versions will include **database integration (MySQL/MongoDB)** and a **JavaFX GUI** for an interactive experience.  

---

## ✨ Features
- 👤 **User Management**
  - Register new users with account number auto-generation.
  - Secure login with AES password encryption.
  - Multiple users supported via in-memory `HashMap`.

- 💰 **Account & Transactions**
  - Deposit, withdraw, and transfer money.
  - Transaction history stored in `ArrayList<Transaction>`.
  - Custom exceptions (e.g., `InsufficientBalanceException`).

- ⏰ **Smart Reminder System**
  - Add bill reminders (type, amount, due date).
  - Background thread checks deadlines using `TimerTask`.

- 📊 **Budget & Expense Tracking**
  - Set monthly salary & budget.
  - Track expenses and alert when 80% of budget is reached.
  - Generate a monthly financial report.

- 🤲 **Donation to Charity Trusts**
  - Users can donate a chosen amount to registered charity trusts.
  - Donation records stored alongside transaction history.
  - Transparency in tracking donation history.

- 🛒 **Grocery Item Management**
  - Add, view, and manage grocery items with price and quantity.
  - Track total grocery expenses and link with budget.
  - Maintain a grocery list (`ArrayList<GroceryItem>`) per user.

- ⚙️ **Technical Highlights**
  - OOP concepts (Encapsulation, Inheritance, Polymorphism).
  - Collections (`ArrayList`, `HashMap`, `HashSet`).
  - Custom Exceptions.
  - Multithreading (`ScheduledExecutorService` for reminders).

---

## 🗂️ Project Structure
