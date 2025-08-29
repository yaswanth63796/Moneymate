import java.util.*;
import java.util.concurrent.*;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.text.DecimalFormat;

// Custom Exceptions
class InsufficientBalanceException extends Exception {
    public InsufficientBalanceException(String message) {
        super(message);
    }
}

class InvalidUserException extends Exception {
    public InvalidUserException(String message) {
        super(message);
    }
}

class InvalidInputException extends Exception {
    public InvalidInputException(String message) {
        super(message);
    }
}

// Model Classes


class User {
    private String name;
    private String email;
    private String phone;
    private String password; // Encrypted
    private String accountNumber;
    private String address;
    private String occupation;
    private int age;
    private Account account;
    private List<Reminder> reminders;
    private Budget budget;
    private List<GroceryItem> groceryItems;
    private List<Donation> donations;
    private DonationGoal donationGoal;

    public User(String name, String email, String phone, String password, String accountNumber,
                String address, String occupation, int age) {
        this.name = name;
        this.email = email;
        this.phone = phone;
        this.password = SecurityService.encrypt(password);
        this.accountNumber = accountNumber;
        this.address = address;
        this.occupation = occupation;
        this.age = age;
        this.account = new Account(accountNumber);
        this.reminders = new ArrayList<>();
        this.budget = new Budget();
        this.groceryItems = new ArrayList<>();
        this.donations = new ArrayList<>();
        this.donationGoal = null;
    }

    // Getters and setters
    public String getName() { return name; }
    public String getEmail() { return email; }
    public String getPhone() { return phone; }
    public String getPassword() { return password; }
    public String getAccountNumber() { return accountNumber; }
    public String getAddress() { return address; }
    public String getOccupation() { return occupation; }
    public int getAge() { return age; }
    public Account getAccount() { return account; }
    public List<Reminder> getReminders() { return reminders; }
    public Budget getBudget() { return budget; }
    public List<GroceryItem> getGroceryItems() { return groceryItems; }
    public List<Donation> getDonations() { return donations; }
    public DonationGoal getDonationGoal() { return donationGoal; }
    public void setDonationGoal(DonationGoal goal) { this.donationGoal = goal; }

    public void addReminder(Reminder reminder) {
        reminders.add(reminder);
    }

    public void addGroceryItem(GroceryItem item) {
        groceryItems.add(item);
        budget.addExpense(item.getPrice()); // also count in budget
    }

    public void removeGroceryItem(int index) {
        if (index >= 0 && index < groceryItems.size()) {
            GroceryItem item = groceryItems.get(index);
            budget.addExpense(-item.getPrice()); // subtract from budget
            groceryItems.remove(index);
        }
    }

    public boolean validatePassword(String password) {
        return this.password.equals(SecurityService.encrypt(password));
    }

    public void addDonation(Donation donation) {
        donations.add(donation);
        budget.addExpense(donation.getAmount()); // donations also part of expenses
        if (donationGoal != null) {
            donationGoal.addDonation(donation.getAmount());
        }
    }

    public double getTotalDonations() {
        return donations.stream().mapToDouble(Donation::getAmount).sum();
    }

    public double getTaxDeductibleDonations() {
        return donations.stream()
                .filter(Donation::isTaxDeductible)
                .mapToDouble(Donation::getAmount)
                .sum();
    }
}

class Account {
    private String accountNumber;
    private double balance;
    private List<Transaction> transactions;

    public Account(String accountNumber) {
        this.accountNumber = accountNumber;
        this.balance = 0.0;
        this.transactions = new ArrayList<>();
    }

    public String getAccountNumber() { return accountNumber; }
    public double getBalance() { return balance; }
    public List<Transaction> getTransactions() { return transactions; }

    public void deposit(double amount, String description) {
        balance += amount;
        transactions.add(new Transaction("DEPOSIT", amount, description));
    }

    public void withdraw(double amount, String description) throws InsufficientBalanceException {
        if (amount > balance) {
            throw new InsufficientBalanceException("Insufficient balance. Current balance: " + balance);
        }
        balance -= amount;
        transactions.add(new Transaction("WITHDRAW", amount, description));
    }

    public void transfer(Account recipient, double amount, String description)
            throws InsufficientBalanceException {
        if (amount > balance) {
            throw new InsufficientBalanceException("Insufficient balance. Current balance: " + balance);
        }
        balance -= amount;
        recipient.balance += amount;
        transactions.add(new Transaction("TRANSFER_TO", amount, description + " to " + recipient.getAccountNumber()));
        recipient.transactions.add(new Transaction("TRANSFER_FROM", amount, description + " from " + accountNumber));
    }
}

class Transaction {
    private String type;
    private double amount;
    private String description;
    private Date timestamp;

    public Transaction(String type, double amount, String description) {
        this.type = type;
        this.amount = amount;
        this.description = description;
        this.timestamp = new Date();
    }

    public String getType() { return type; }
    public double getAmount() { return amount; }
    public String getDescription() { return description; }
    public Date getTimestamp() { return timestamp; }

    @Override
    public String toString() {
        return String.format("[%s] %s: $%.2f - %s", timestamp, type, amount, description);
    }
}

class Reminder {
    private String billType;
    private double amount;
    private LocalDate dueDate;
    private String description;
    private String priority;
    private boolean isPaid;

    public Reminder(String billType, double amount, LocalDate dueDate, String description, String priority) {
        this.billType = billType;
        this.amount = amount;
        this.dueDate = dueDate;
        this.description = description;
        this.priority = priority;
        this.isPaid = false;
    }

    public String getBillType() { return billType; }
    public double getAmount() { return amount; }
    public LocalDate getDueDate() { return dueDate; }
    public String getDescription() { return description; }
    public String getPriority() { return priority; }
    public boolean isPaid() { return isPaid; }
    public void markAsPaid() { this.isPaid = true; }

    public boolean isDue() {
        return LocalDate.now().isAfter(dueDate) || LocalDate.now().isEqual(dueDate);
    }

    public boolean isDueInDays(int days) {
        return LocalDate.now().plusDays(days).isAfter(dueDate) || LocalDate.now().plusDays(days).isEqual(dueDate);
    }

    @Override
    public String toString() {
        return String.format("%s: $%.2f due on %s [%s] - %s (%s)",
                billType, amount, dueDate.toString(), isPaid ? "PAID" : "PENDING", description, priority);
    }
}

class GroceryItem {
    private String name;
    private String category;
    private double price;
    private int quantity;
    private LocalDate purchaseDate;

    public GroceryItem(String name, String category, double price, int quantity, LocalDate purchaseDate) {
        this.name = name;
        this.category = category;
        this.price = price;
        this.quantity = quantity;
        this.purchaseDate = purchaseDate;
    }

    public String getName() { return name; }
    public String getCategory() { return category; }
    public double getPrice() { return price; }
    public int getQuantity() { return quantity; }
    public LocalDate getPurchaseDate() { return purchaseDate; }

    @Override
    public String toString() {
        return String.format("%s (%s) - $%.2f x %d = $%.2f on %s",
                name, category, price, quantity, price * quantity, purchaseDate.toString());
    }
}

class Budget {
    private double monthlySalary;
    private double budgetLimit;
    private double currentExpenses;
    private Map<String, Double> categoryExpenses;
    private Map<Integer, Double> weeklyExpenses; // store actual weekly spending

    public Budget() {
        this.monthlySalary = 0.0;
        this.budgetLimit = 0.0;
        this.currentExpenses = 0.0;
        this.categoryExpenses = new HashMap<>();
        this.weeklyExpenses = new HashMap<>();
        for (int i = 1; i <= 4; i++) {
            weeklyExpenses.put(i, 0.0); // initialize all weeks with 0 spent
        }
    }

    public double getMonthlySalary() { return monthlySalary; }
    public void setMonthlySalary(double monthlySalary) { this.monthlySalary = monthlySalary; }

    public double getBudgetLimit() { return budgetLimit; }
    public void setBudgetLimit(double budgetLimit) { this.budgetLimit = budgetLimit; }

    public double getCurrentExpenses() { return currentExpenses; }

    public void addExpense(double amount) {
        this.currentExpenses += amount;
    }

    public void addCategoryExpense(String category, double amount) {
        categoryExpenses.put(category, categoryExpenses.getOrDefault(category, 0.0) + amount);
        addExpense(amount);
    }

    public Map<String, Double> getCategoryExpenses() {
        return categoryExpenses;
    }

    public void resetExpenses() {
        this.currentExpenses = 0.0;
        this.categoryExpenses.clear();
        for (int i = 1; i <= 4; i++) {
            weeklyExpenses.put(i, 0.0);
        }
    }

    public double getRemainingBudget() {
        return budgetLimit - currentExpenses;
    }

    public boolean isBudgetExceeded() {
        return currentExpenses > budgetLimit;
    }

    public boolean isBudgetNearExceed() {
        return currentExpenses >= budgetLimit * 0.8;
    }

    // Weekly allocation should be based on Budget Limit (not salary)
    public Map<Integer, Double> getWeeklyBudgetAllocation() {
        Map<Integer, Double> weeklyBudget = new HashMap<>();
        double weeklyLimit = budgetLimit / 4; // split budget equally into 4 weeks
        for (int i = 1; i <= 4; i++) {
            weeklyBudget.put(i, weeklyLimit);
        }
        return weeklyBudget;
    }

    // Record actual money spent in a specific week
    public void addWeeklyExpense(int week, double amount) {
        if (week >= 1 && week <= 4) {
            weeklyExpenses.put(week, weeklyExpenses.get(week) + amount);
            addExpense(amount);
        } else {
            System.out.println("❌ Invalid week! Please enter between 1 and 4.");
        }
    }

    public Map<Integer, Double> getWeeklyExpenses() {
        return weeklyExpenses;
    }
}

class Donation {
    private String charityName;
    private String charityType;
    private double amount;
    private LocalDate donationDate;
    private String paymentMethod;
    private boolean taxDeductible;
    private String receiptId;
    private String description;

    public Donation(String charityName, String charityType, double amount, LocalDate donationDate,
                    String paymentMethod, boolean taxDeductible, String receiptId, String description) {
        this.charityName = charityName;
        this.charityType = charityType;
        this.amount = amount;
        this.donationDate = donationDate;
        this.paymentMethod = paymentMethod;
        this.taxDeductible = taxDeductible;
        this.receiptId = receiptId;
        this.description = description;
    }

    // Getters
    public String getCharityName() { return charityName; }
    public String getCharityType() { return charityType; }
    public double getAmount() { return amount; }
    public LocalDate getDonationDate() { return donationDate; }
    public String getPaymentMethod() { return paymentMethod; }
    public boolean isTaxDeductible() { return taxDeductible; }
    public String getReceiptId() { return receiptId; }
    public String getDescription() { return description; }

    @Override
    public String toString() {
        return String.format("%s: $%.2f on %s (%s)", charityName, amount, donationDate.toString(), charityType);
    }
}

class DonationGoal {
    private double targetPercentage;
    private String timeFrame;
    private LocalDate startDate;
    private LocalDate endDate;
    private double amountDonated;
    private String preferredCategories;

    public DonationGoal(double targetPercentage, String timeFrame, LocalDate startDate,
                        LocalDate endDate, String preferredCategories) {
        this.targetPercentage = targetPercentage;
        this.timeFrame = timeFrame;
        this.startDate = startDate;
        this.endDate = endDate;
        this.amountDonated = 0.0;
        this.preferredCategories = preferredCategories;
    }

    // Getters and setters
    public double getTargetPercentage() { return targetPercentage; }
    public String getTimeFrame() { return timeFrame; }
    public LocalDate getStartDate() { return startDate; }
    public LocalDate getEndDate() { return endDate; }
    public double getAmountDonated() { return amountDonated; }
    public String getPreferredCategories() { return preferredCategories; }

    public void addDonation(double amount) {
        this.amountDonated += amount;
    }

    public double getTargetAmount(double income) {
        return income * (targetPercentage / 100);
    }

    public double getProgressPercentage(double income) {
        double target = getTargetAmount(income);
        if (target == 0) return 0;
        return (amountDonated / target) * 100;
    }

    public boolean isGoalAchieved(double income) {
        return amountDonated >= getTargetAmount(income);
    }
}

// Service Classes
class SecurityService {
    public static String encrypt(String plainText) {
        try {
            // Simple encryption for console app
            return Base64.getEncoder().encodeToString(plainText.getBytes());
        } catch (Exception e) {
            return plainText; // Fallback
        }
    }

    public static String decrypt(String encryptedText) {
        try {
            return new String(Base64.getDecoder().decode(encryptedText));
        } catch (Exception e) {
            return encryptedText; // Fallback
        }
    }
}

class AccountService {
    public static void deposit(User user, double amount, String description) {
        user.getAccount().deposit(amount, description);
        System.out.println("Deposit successful. New balance: $" + user.getAccount().getBalance());
    }

    public static void withdraw(User user, double amount, String description)
            throws InsufficientBalanceException {
        user.getAccount().withdraw(amount, description);
        System.out.println("Withdrawal successful. New balance: $" + user.getAccount().getBalance());
    }

    public static void transfer(User fromUser, Map<String, User> users,
                                String toAccountNumber, double amount, String description)
            throws InsufficientBalanceException, InvalidUserException {
        if (!users.containsKey(toAccountNumber)) {
            throw new InvalidUserException("Recipient account not found: " + toAccountNumber);
        }

        User toUser = users.get(toAccountNumber);
        fromUser.getAccount().transfer(toUser.getAccount(), amount, description);
        System.out.println("Transfer successful. New balance: $" + fromUser.getAccount().getBalance());
    }

    public static void showTransactionHistory(User user) {
        System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=== Transaction History ===" + ConsoleColors.RESET);
        if (user.getAccount().getTransactions().isEmpty()) {
            System.out.println("No transactions found.");
            return;
        }

        for (Transaction transaction : user.getAccount().getTransactions()) {
            System.out.println(transaction);
        }
    }
}

class ReminderService {
    private static ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(1);

    public static void addReminder(User user, String billType, double amount, LocalDate dueDate,
                                   String description, String priority) {
        Reminder reminder = new Reminder(billType, amount, dueDate, description, priority);
        user.addReminder(reminder);
        System.out.println("Reminder added: " + reminder);
    }

    public static void viewReminders(User user) {
        System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=== Payment Reminders ===" + ConsoleColors.RESET);
        if (user.getReminders().isEmpty()) {
            System.out.println("No reminders found.");
            return;
        }

        // Display reminders in a table format
        System.out.println("+----+----------------------+-----------+------------+---------------------+----------+---------+");
        System.out.println("| No | Bill Type            | Amount    | Due Date   | Description         | Priority | Status  |");
        System.out.println("+----+----------------------+-----------+------------+---------------------+----------+---------+");

        int i = 1;
        for (Reminder reminder : user.getReminders()) {
            String status = reminder.isPaid() ?
                    ConsoleColors.GREEN + "PAID" + ConsoleColors.RESET :
                    (reminder.isDue() ? ConsoleColors.RED + "DUE" + ConsoleColors.RESET : ConsoleColors.YELLOW + "PENDING" + ConsoleColors.RESET);

            System.out.printf("| %-2d | %-20s | $%-8.2f | %-10s | %-19s | %-8s | %-7s |\n",
                    i++,
                    reminder.getBillType(),
                    reminder.getAmount(),
                    reminder.getDueDate().toString(),
                    reminder.getDescription().length() > 19 ? reminder.getDescription().substring(0, 16) + "..." : reminder.getDescription(),
                    reminder.getPriority(),
                    status);
        }
        System.out.println("+----+----------------------+-----------+------------+---------------------+----------+---------+");
    }

    public static void startReminderChecker(Map<String, User> users) {
        Runnable reminderCheck = () -> {
            for (User user : users.values()) {
                for (Reminder reminder : user.getReminders()) {
                    if (reminder.isDue() && !reminder.isPaid()) {
                        System.out.println("\n" + ConsoleColors.RED_BACKGROUND + "[REMINDER] " + user.getName() +
                                ", your bill '" + reminder.getBillType() +
                                "' for $" + reminder.getAmount() + " is due!" + ConsoleColors.RESET);
                    } else if (reminder.isDueInDays(2) && !reminder.isPaid()) {
                        System.out.println("\n" + ConsoleColors.YELLOW_BACKGROUND + "[REMINDER] " + user.getName() +
                                ", your bill '" + reminder.getBillType() +
                                "' for $" + reminder.getAmount() + " is due in 2 days!" + ConsoleColors.RESET);
                    }
                }
            }
        };

        // Check every minute for demonstration
        scheduler.scheduleAtFixedRate(reminderCheck, 0, 1, TimeUnit.MINUTES);
    }

    public static void stopReminderChecker() {
        scheduler.shutdown();
    }

    public static void markReminderAsPaid(User user, int index) {
        if (index < 1 || index > user.getReminders().size()) {
            System.out.println("Invalid reminder index.");
            return;
        }

        Reminder reminder = user.getReminders().get(index - 1);
        reminder.markAsPaid();
        System.out.println("Marked reminder as paid: " + reminder.getBillType());
    }
}

class BudgetService {

    private static Scanner scanner = new Scanner(System.in);

    public static void setMonthlyBudget(User user, double salary, double budgetLimit) {
        user.getBudget().setMonthlySalary(salary);
        user.getBudget().setBudgetLimit(budgetLimit);
        System.out.println("\n✅ Budget set successfully.");
        System.out.println("Monthly Salary: $" + salary);
        System.out.println("Budget Limit: $" + budgetLimit);

        // Show weekly budget allocation
        Map<Integer, Double> weeklyBudget = user.getBudget().getWeeklyBudgetAllocation();
        System.out.println("\n=== Weekly Budget Allocation ===");
        for (Map.Entry<Integer, Double> entry : weeklyBudget.entrySet()) {
            System.out.printf("Week %d: $%.2f\n", entry.getKey(), entry.getValue());
        }
    }

    // Let user enter weekly expenses
    public static void recordWeeklyExpenses(User user) {
        Budget budget = user.getBudget();
        System.out.println("\n=== Enter Weekly Expenses ===");
        for (int i = 1; i <= 4; i++) {
            System.out.print("Enter expense for Week " + i + ": $");
            double expense = scanner.nextDouble();
            budget.addWeeklyExpense(i, expense);
        }
        System.out.println("✅ Weekly expenses recorded successfully!");
    }

    public static void showBudgetReport(User user) {
        Budget budget = user.getBudget();
        System.out.println("\n=== Budget Report ===");
        System.out.println("Monthly Salary: $" + budget.getMonthlySalary());
        System.out.println("Budget Limit: $" + budget.getBudgetLimit());
        System.out.println("Current Expenses: $" + budget.getCurrentExpenses());
        System.out.println("Remaining Budget: $" + budget.getRemainingBudget());

        if (budget.isBudgetExceeded()) {
            System.out.println("⚠️  WARNING: You have exceeded your budget!");
        } else if (budget.isBudgetNearExceed()) {
            System.out.println("⚠️  WARNING: You have used 80% or more of your budget!");
        }

        // Show weekly budget status
        Map<Integer, Double> weeklyBudget = budget.getWeeklyBudgetAllocation();
        Map<Integer, Double> weeklyExpenses = budget.getWeeklyExpenses();

        System.out.println("\n=== Weekly Budget Status ===");
        for (int i = 1; i <= 4; i++) {
            double budgetAmount = weeklyBudget.get(i);
            double spent = weeklyExpenses.get(i);
            double remaining = budgetAmount - spent;
            System.out.printf("Week %d: Budget: $%.2f | Spent: $%.2f | Remaining: $%.2f\n",
                    i, budgetAmount, spent, remaining);
        }

        // Show category-wise expenses (if any)
        if (!budget.getCategoryExpenses().isEmpty()) {
            System.out.println("\n=== Category-wise Expenses ===");
            for (Map.Entry<String, Double> entry : budget.getCategoryExpenses().entrySet()) {
                System.out.printf("%-15s: $%.2f\n", entry.getKey(), entry.getValue());
            }
        }
    }
}

class GroceryService {
    public static void addGroceryItem(User user, String name, String category, double price, int quantity, LocalDate purchaseDate) {
        GroceryItem item = new GroceryItem(name, category, price, quantity, purchaseDate);
        user.addGroceryItem(item);
        user.getBudget().addCategoryExpense(category, price * quantity);
        System.out.println("Grocery item added: " + item);
    }

    public static void viewGroceryItems(User user) {
        System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=== Grocery Items ===" + ConsoleColors.RESET);
        if (user.getGroceryItems().isEmpty()) {
            System.out.println("No grocery items found.");
            return;
        }

        // Display grocery items in a table format
        System.out.println("+----+----------------------+----------------------+-----------+----------+------------+");
        System.out.println("| No | Name                 | Category             | Price     | Quantity | Total      |");
        System.out.println("+----+----------------------+----------------------+-----------+----------+------------+");

        int i = 1;
        double total = 0;
        for (GroceryItem item : user.getGroceryItems()) {
            double itemTotal = item.getPrice() * item.getQuantity();
            total += itemTotal;
            System.out.printf("| %-2d | %-20s | %-20s | $%-8.2f | %-8d | $%-10.2f |\n",
                    i++,
                    item.getName(),
                    item.getCategory(),
                    item.getPrice(),
                    item.getQuantity(),
                    itemTotal);
        }
        System.out.println("+----+----------------------+----------------------+-----------+----------+------------+");
        System.out.printf("| %-68s | $%-10.2f |\n", "TOTAL", total);
        System.out.println("+--------------------------------------------------------------------+------------+");
    }
}

class DonationService {
    private static final List<String> CHARITY_TYPES = Arrays.asList(
            "Education", "Health", "Environment", "Animal Welfare",
            "Human Rights", "Disaster Relief", "Religious", "Other"
    );

    private static final List<String> PAYMENT_METHODS = Arrays.asList(
            "Credit Card", "Debit Card", "Bank Transfer", "Cash", "Check", "Online Payment"
    );

    public static void addDonation(User user, String charityName, String charityType, double amount,
                                   LocalDate donationDate, String paymentMethod, boolean taxDeductible,
                                   String receiptId, String description) {
        Donation donation = new Donation(charityName, charityType, amount, donationDate,
                paymentMethod, taxDeductible, receiptId, description);
        user.addDonation(donation);
        System.out.println("Donation recorded successfully!");
    }

    public static void viewDonations(User user) {
        System.out.println("\n=== Your Donations ===");
        if (user.getDonations().isEmpty()) {
            System.out.println("No donations recorded yet.");
            return;
        }

        System.out.println("+----+----------------------+----------------------+-----------+------------+----------------+--------------+");
        System.out.println("| No | Charity Name         | Type                 | Amount    | Date       | Payment Method | Tax Deduct.  |");
        System.out.println("+----+----------------------+----------------------+-----------+------------+----------------+--------------+");

        int i = 1;
        for (Donation donation : user.getDonations()) {
            System.out.printf("| %-2d | %-20s | %-20s | $%-8.2f | %-10s | %-14s | %-12s |\n",
                    i++, donation.getCharityName(), donation.getCharityType(),
                    donation.getAmount(), donation.getDonationDate().toString(),
                    donation.getPaymentMethod(), donation.isTaxDeductible() ? "Yes" : "No");
        }
        System.out.println("+----+----------------------+----------------------+-----------+------------+----------------+--------------+");

        System.out.printf("Total Donations: $%.2f\n", user.getTotalDonations());
        System.out.printf("Tax Deductible Donations: $%.2f\n", user.getTaxDeductibleDonations());
    }

    public static void generateTaxReport(User user, int year) {
        System.out.println("\n=== Tax Deduction Report for " + year + " ===");

        double totalDeductible = user.getDonations().stream()
                .filter(d -> d.isTaxDeductible() && d.getDonationDate().getYear() == year)
                .mapToDouble(Donation::getAmount)
                .sum();

        if (totalDeductible == 0) {
            System.out.println("No tax-deductible donations for " + year);
            return;
        }

        System.out.println("+----------------------+----------------------+-----------+------------+--------------+");
        System.out.println("| Charity Name         | Type                 | Amount    | Date       | Receipt ID   |");
        System.out.println("+----------------------+----------------------+-----------+------------+--------------+");

        for (Donation donation : user.getDonations()) {
            if (donation.isTaxDeductible() && donation.getDonationDate().getYear() == year) {
                System.out.printf("| %-20s | %-20s | $%-8.2f | %-10s | %-12s |\n",
                        donation.getCharityName(), donation.getCharityType(),
                        donation.getAmount(), donation.getDonationDate().toString(),
                        donation.getReceiptId());
            }
        }
        System.out.println("+----------------------+----------------------+-----------+------------+--------------+");
        System.out.printf("Total Tax-Deductible Donations for %d: $%.2f\n", year, totalDeductible);
    }

    public static void setDonationGoal(User user, double targetPercentage, String timeFrame,
                                       LocalDate startDate, LocalDate endDate, String preferredCategories) {
        DonationGoal goal = new DonationGoal(targetPercentage, timeFrame, startDate, endDate, preferredCategories);
        user.setDonationGoal(goal);
        System.out.println("Donation goal set successfully!");
    }

    public static void viewDonationGoalProgress(User user) {
        if (user.getDonationGoal() == null) {
            System.out.println("No donation goal set yet.");
            return;
        }

        DonationGoal goal = user.getDonationGoal();
        double income = user.getBudget().getMonthlySalary() * 12; // Annual income
        double targetAmount = goal.getTargetAmount(income);
        double progress = goal.getProgressPercentage(income);

        System.out.println("\n=== Donation Goal Progress ===");
        System.out.printf("Target: %.1f%% of income ($%.2f)\n", goal.getTargetPercentage(), targetAmount);
        System.out.printf("Time Frame: %s (%s to %s)\n", goal.getTimeFrame(),
                goal.getStartDate().toString(), goal.getEndDate().toString());
        System.out.printf("Amount Donated: $%.2f\n", goal.getAmountDonated());
        System.out.printf("Progress: %.1f%%\n", progress);

        if (goal.isGoalAchieved(income)) {
            System.out.println("🎉 Congratulations! You've achieved your donation goal!");
        } else {
            double remaining = targetAmount - goal.getAmountDonated();
            System.out.printf("You need to donate $%.2f more to reach your goal.\n", remaining);
        }
    }

    public static List<String> getCharityTypes() {
        return CHARITY_TYPES;
    }

    public static List<String> getPaymentMethods() {
        return PAYMENT_METHODS;
    }
}

class ConsoleColors {
    // Reset
    public static final String RESET = "\033[0m";  // Text Reset

    // Regular Colors
    public static final String BLACK = "\033[0;30m";   // BLACK
    public static final String RED = "\033[0;31m";     // RED
    public static final String GREEN = "\033[0;32m";   // GREEN
    public static final String YELLOW = "\033[0;33m";  // YELLOW
    public static final String BLUE = "\033[0;34m";    // BLUE
    public static final String PURPLE = "\033[0;35m";  // PURPLE
    public static final String CYAN = "\033[0;36m";    // CYAN
    public static final String WHITE = "\033[0;37m";   // WHITE

    // Bold
    public static final String BLACK_BOLD = "\033[1;30m";  // BLACK
    public static final String RED_BOLD = "\033[1;31m";    // RED
    public static final String GREEN_BOLD = "\033[1;32m";  // GREEN
    public static final String YELLOW_BOLD = "\033[1;33m"; // YELLOW
    public static final String BLUE_BOLD = "\033[1;34m";   // BLUE
    public static final String PURPLE_BOLD = "\033[1;35m"; // PURPLE
    public static final String CYAN_BOLD = "\033[1;36m";   // CYAN
    public static final String WHITE_BOLD = "\033[1;37m";  // WHITE

    // Background
    public static final String BLACK_BACKGROUND = "\033[40m";  // BLACK
    public static final String RED_BACKGROUND = "\033[41m";    // RED
    public static final String GREEN_BACKGROUND = "\033[42m";  // GREEN
    public static final String YELLOW_BACKGROUND = "\033[43m"; // YELLOW
    public static final String BLUE_BACKGROUND = "\033[44m";   // BLUE
    public static final String PURPLE_BACKGROUND = "\033[45m"; // PURPLE
    public static final String CYAN_BACKGROUND = "\033[46m";   // CYAN
    public static final String WHITE_BACKGROUND = "\033[47m";  // WHITE
}

class InputValidator {
    public static double getValidAmount(Scanner scanner, String prompt) throws InvalidInputException {
        System.out.print(prompt);
        try {
            double amount = scanner.nextDouble();
            scanner.nextLine(); // Consume newline
            if (amount <= 0) {
                throw new InvalidInputException("Amount must be positive.");
            }
            return amount;
        } catch (InputMismatchException e) {
            scanner.nextLine(); // Clear invalid input
            throw new InvalidInputException("Invalid amount. Please enter a valid number.");
        }
    }

    public static LocalDate getValidDate(Scanner scanner, String prompt) throws InvalidInputException {
        System.out.print(prompt + " (YYYY-MM-DD): ");
        String dateStr = scanner.nextLine();
        try {
            return LocalDate.parse(dateStr, DateTimeFormatter.ISO_DATE);
        } catch (DateTimeParseException e) {
            throw new InvalidInputException("Invalid date format. Please use YYYY-MM-DD.");
        }
    }

    public static int getValidInt(Scanner scanner, String prompt) throws InvalidInputException {
        System.out.print(prompt);
        try {
            int value = scanner.nextInt();
            scanner.nextLine(); // Consume newline
            if (value <= 0) {
                throw new InvalidInputException("Value must be positive.");
            }
            return value;
        } catch (InputMismatchException e) {
            scanner.nextLine(); // Clear invalid input
            throw new InvalidInputException("Invalid input. Please enter a valid integer.");
        }
    }

    public static String getValidEmail(Scanner scanner, String prompt) throws InvalidInputException {
        System.out.print(prompt);
        String email = scanner.nextLine();
        if (!email.matches("^[A-Za-z0-9+_.-]+@(.+)$")) {
            throw new InvalidInputException("Invalid email format. Please enter a valid email.");
        }
        return email;
    }

    public static String getValidPhone(Scanner scanner, String prompt) throws InvalidInputException {
        System.out.print(prompt);
        String phone = scanner.nextLine();
        if (!phone.matches("^[0-9]{10}$")) {
            throw new InvalidInputException("Invalid phone number. Please enter a 10-digit number.");
        }
        return phone;
    }

    public static String getValidAccountNumber(Scanner scanner, String prompt, Map<String, User> users) throws InvalidInputException {
        System.out.print(prompt);
        String accountNumber = scanner.nextLine();
        if (users.containsKey(accountNumber)) {
            throw new InvalidInputException("Account number already exists. Please try a different one.");
        }
        if (!accountNumber.matches("^[0-9]{9,12}$")) {
            throw new InvalidInputException("Invalid account number. Please enter a 9-12 digit number.");
        }
        return accountNumber;
    }
}








// Main Application
public class Project1 {
    private static Map<String, User> users = new HashMap<>();
    private static User currentUser = null;
    private static Scanner scanner = new Scanner(System.in);
    private static boolean running = true;
    private static final DecimalFormat df = new DecimalFormat("0.00");

    public static void main(String[] args) {
        // Initialize with some sample data
        initializeSampleData();

        // Start background services
        ReminderService.startReminderChecker(users);
        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            ReminderService.stopReminderChecker();
        }));

        // Main application loop
        while (running) {
            if (currentUser == null) {
                showMainMenu();
            } else {
                showUserMenu();
            }
        }

        scanner.close();
        System.out.println("Thank you for using Smart Finance Manager!");
    }

    // ================= BUDGET METHODS ===================
    private static void setBudget() {
        try {
            double salary = InputValidator.getValidAmount(scanner, "Enter monthly salary: $");
            double limit = InputValidator.getValidAmount(scanner, "Enter monthly budget limit: $");

            BudgetService.setMonthlyBudget(currentUser, salary, limit);
        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void recordWeeklyExpenses() {
        BudgetService.recordWeeklyExpenses(currentUser);
    }

    private static void showBudgetReport() {
        BudgetService.showBudgetReport(currentUser);
    }

    private static void initializeSampleData() {
        User user1 = new User("John Doe", "john@example.com", "1234567890", "password123", "ACC001",
                "123 Main St, City", "Software Engineer", 30);
        User user2 = new User("Jane Smith", "jane@example.com", "0987654321", "password456", "ACC002",
                "456 Oak Ave, Town", "Teacher", 28);

        users.put("ACC001", user1);
        users.put("ACC002", user2);

        // Add sample transactions
        try {
            user1.getAccount().deposit(1500.0, "Initial deposit");
            user1.getAccount().withdraw(200.0, "Grocery shopping");
            user1.getAccount().transfer(user2.getAccount(), 100.0, "Dinner payment");
        } catch (InsufficientBalanceException e) {
            System.out.println("Error initializing sample data: " + e.getMessage());
        }

        // Set sample budget
        user1.getBudget().setMonthlySalary(3000.0);
        user1.getBudget().setBudgetLimit(2000.0);

        // Add sample reminders
        user1.addReminder(new Reminder("Electricity Bill", 75.0, LocalDate.now().plusDays(5),
                "Monthly electricity bill", "High"));
        user1.addReminder(new Reminder("Internet Bill", 45.0, LocalDate.now().plusDays(10),
                "Monthly internet subscription", "Medium"));

        // Add sample grocery items
        user1.addGroceryItem(new GroceryItem("Apples", "Fruits", 2.50, 5, LocalDate.now()));
        user1.addGroceryItem(new GroceryItem("Milk", "Dairy", 3.00, 2, LocalDate.now()));
        user1.addGroceryItem(new GroceryItem("Bread", "Bakery", 2.00, 3, LocalDate.now()));

        // Add sample donations
        user1.addDonation(new Donation("Red Cross", "Disaster Relief", 100.0,
                LocalDate.now().minusMonths(2), "Credit Card",
                true, "RC12345", "Monthly donation"));
        user1.addDonation(new Donation("Local Food Bank", "Other", 50.0,
                LocalDate.now().minusMonths(1), "Cash",
                true, "FB67890", "Thanksgiving donation"));

        // Set sample donation goal
        DonationGoal goal = new DonationGoal(5.0, "Yearly",
                LocalDate.now().withDayOfYear(1),
                LocalDate.now().withDayOfYear(365),
                "Education, Health");
        goal.addDonation(150.0); // Add existing donations to goal
        user1.setDonationGoal(goal);
    }

    private static void recordDonation() {
        try {
            System.out.println("\n=== Record Donation ===");

            System.out.print("Enter charity name: ");
            String charityName = scanner.nextLine();

            List<String> charityTypes = DonationService.getCharityTypes();
            System.out.println("Select charity type:");
            for (int i = 0; i < charityTypes.size(); i++) {
                System.out.println((i + 1) + ". " + charityTypes.get(i));
            }
            int typeChoice = InputValidator.getValidInt(scanner, "Enter choice: ");
            String charityType = charityTypes.get(typeChoice - 1);

            double amount = InputValidator.getValidAmount(scanner, "Enter donation amount: $");
            LocalDate donationDate = InputValidator.getValidDate(scanner, "Enter donation date");

            List<String> paymentMethods = DonationService.getPaymentMethods();
            System.out.println("Select payment method:");
            for (int i = 0; i < paymentMethods.size(); i++) {
                System.out.println((i + 1) + ". " + paymentMethods.get(i));
            }
            int methodChoice = InputValidator.getValidInt(scanner, "Enter choice: ");
            String paymentMethod = paymentMethods.get(methodChoice - 1);

            System.out.print("Is this tax deductible? (yes/no): ");
            boolean taxDeductible = scanner.nextLine().equalsIgnoreCase("yes");

            String receiptId = "";
            if (taxDeductible) {
                System.out.print("Enter receipt ID (if any): ");
                receiptId = scanner.nextLine();
            }

            System.out.print("Enter description (optional): ");
            String description = scanner.nextLine();

            DonationService.addDonation(currentUser, charityName, charityType, amount,
                    donationDate, paymentMethod, taxDeductible,
                    receiptId, description);
        } catch (InvalidInputException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void generateTaxReport() {
        try {
            int currentYear = LocalDate.now().getYear();
            System.out.print("Enter year for tax report (" + (currentYear - 1) + " or " + currentYear + "): ");
            int year = scanner.nextInt();
            scanner.nextLine();

            DonationService.generateTaxReport(currentUser, year);
        } catch (InputMismatchException e) {
            System.out.println("Invalid year. Please enter a valid number.");
            scanner.nextLine();
        }
    }

    private static void setDonationGoal() {
        try {
            System.out.println("\n=== Set Donation Goal ===");

            double targetPercentage = InputValidator.getValidAmount(scanner,
                    "Enter target donation percentage of income: ");

            System.out.println("Select time frame:");
            System.out.println("1. Monthly");
            System.out.println("2. Quarterly");
            System.out.println("3. Yearly");
            int timeChoice = InputValidator.getValidInt(scanner, "Enter choice: ");
            String timeFrame = "";
            switch (timeChoice) {
                case 1:
                    timeFrame = "Monthly";
                    break;
                case 2:
                    timeFrame = "Quarterly";
                    break;
                case 3:
                    timeFrame = "Yearly";
                    break;
                default:
                    throw new InvalidInputException("Invalid time frame choice.");
            }

            LocalDate startDate = InputValidator.getValidDate(scanner, "Enter start date");
            LocalDate endDate = InputValidator.getValidDate(scanner, "Enter end date");

            List<String> charityTypes = DonationService.getCharityTypes();
            System.out.println("Select preferred charity types (comma-separated numbers):");
            for (int i = 0; i < charityTypes.size(); i++) {
                System.out.println((i + 1) + ". " + charityTypes.get(i));
            }
            System.out.print("Enter choices (e.g., 1,3,5): ");
            String choices = scanner.nextLine();

            StringBuilder preferredCategories = new StringBuilder();
            String[] choiceArray = choices.split(",");
            for (String choice : choiceArray) {
                try {
                    int index = Integer.parseInt(choice.trim()) - 1;
                    if (index >= 0 && index < charityTypes.size()) {
                        if (preferredCategories.length() > 0) {
                            preferredCategories.append(", ");
                        }
                        preferredCategories.append(charityTypes.get(index));
                    }
                } catch (NumberFormatException e) {
                    // Skip invalid entries
                }
            }

            DonationService.setDonationGoal(currentUser, targetPercentage, timeFrame,
                    startDate, endDate, preferredCategories.toString());
        } catch (InvalidInputException e) {
            System.out.println("Error: " + e.getMessage());
        }
    }

    private static void showMainMenu() {
        System.out.println("\n" + ConsoleColors.CYAN_BACKGROUND + ConsoleColors.BLACK_BOLD +
                "=== Smart Finance & Payment Manager ===" + ConsoleColors.RESET);
        System.out.println("1. Register");
        System.out.println("2. Login");
        System.out.println("3. Exit");

        System.out.print("Choose an option: ");

        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1:
                    registerUser();
                    break;
                case 2:
                    loginUser();
                    break;
                case 3:
                    running = false;
                    break;
                default:
                    System.out.println(ConsoleColors.RED + "Invalid option. Please try again." + ConsoleColors.RESET);
            }
        } catch (InputMismatchException e) {
            System.out.println(ConsoleColors.RED + "Invalid input. Please enter a number." + ConsoleColors.RESET);
            scanner.nextLine(); // Clear invalid input
        }
    }

    private static void showUserMenu() {
        System.out.println("\n" + ConsoleColors.GREEN_BOLD + "=== Welcome, " + currentUser.getName() + " ===" + ConsoleColors.RESET);
        System.out.println("1. Deposit");
        System.out.println("2. Withdraw");
        System.out.println("3. Transfer");
        System.out.println("4. Transaction History");
        System.out.println("5. Add Reminder");
        System.out.println("6. View Reminders");
        System.out.println("7. Mark Reminder as Paid");
        System.out.println("8. Set Budget");
        System.out.println("9. Record Weekly Expenses");
        System.out.println("10. View Budget Report");
        System.out.println("11. Add Grocery Item");
        System.out.println("12. View Grocery Items");
        System.out.println("13. Remove Grocery Item");
        System.out.println("14. Record Donation");
        System.out.println("15. View Donations");
        System.out.println("16. Generate Tax Report");
        System.out.println("17. Set Donation Goal");
        System.out.println("18. View Donation Progress");
        System.out.println("19. Logout");
        System.out.print("Choose an option: ");

        try {
            int choice = scanner.nextInt();
            scanner.nextLine(); // Consume newline

            switch (choice) {
                case 1: depositMoney(); break;
                case 2: withdrawMoney(); break;
                case 3: transferMoney(); break;
                case 4: AccountService.showTransactionHistory(currentUser); break;
                case 5: addReminder(); break;
                case 6: ReminderService.viewReminders(currentUser); break;
                case 7: markReminderAsPaid(); break;
                case 8: setBudget(); break;
                case 9: recordWeeklyExpenses(); break;
                case 10: showBudgetReport(); break;
                case 11: addGroceryItem(); break;
                case 12: GroceryService.viewGroceryItems(currentUser); break;
                case 13: removeGroceryItem(); break;
                case 14: recordDonation(); break;
                case 15: DonationService.viewDonations(currentUser); break;
                case 16: generateTaxReport(); break;
                case 17: setDonationGoal(); break;
                case 18: DonationService.viewDonationGoalProgress(currentUser); break;
                case 19: currentUser = null; System.out.println("Logged out successfully."); break;
                default: System.out.println(ConsoleColors.RED + "Invalid option. Please try again." + ConsoleColors.RESET);
            }
        } catch (InputMismatchException e) {
            System.out.println(ConsoleColors.RED + "Invalid input. Please enter a number." + ConsoleColors.RESET);
            scanner.nextLine(); // Clear invalid input
        } catch (Exception e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }


    private static void registerUser() {
        System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=== User Registration ===" + ConsoleColors.RESET);

        try {
            System.out.print("Enter name: ");
            String name = scanner.nextLine();

            String email = InputValidator.getValidEmail(scanner, "Enter email: ");

            String phone = InputValidator.getValidPhone(scanner, "Enter phone: ");

            System.out.print("Enter password: ");
            String password = scanner.nextLine();

            String accountNumber = InputValidator.getValidAccountNumber(scanner, "Enter account number: ", users);

            System.out.print("Enter address: ");
            String address = scanner.nextLine();

            System.out.print("Enter occupation: ");
            String occupation = scanner.nextLine();

            int age = InputValidator.getValidInt(scanner, "Enter age: ");

            User newUser = new User(name, email, phone, password, accountNumber, address, occupation, age);
            users.put(accountNumber, newUser);
            System.out.println(ConsoleColors.GREEN + "Registration successful! You can now login." + ConsoleColors.RESET);

            // Show user details
            System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=== Registration Details ===" + ConsoleColors.RESET);
            System.out.println("Name: " + name);
            System.out.println("Email: " + email);
            System.out.println("Phone: " + phone);
            System.out.println("Account Number: " + accountNumber);
            System.out.println("Address: " + address);
            System.out.println("Occupation: " + occupation);
            System.out.println("Age: " + age);

        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void loginUser() {
        System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=== User Login ===" + ConsoleColors.RESET);
        System.out.print("Enter account number: ");
        String accountNumber = scanner.nextLine();

        System.out.print("Enter password: ");
        String password = scanner.nextLine();

        if (!users.containsKey(accountNumber)) {
            System.out.println(ConsoleColors.RED + "Account not found. Please check your account number." + ConsoleColors.RESET);
            return;
        }

        User user = users.get(accountNumber);
        if (user.validatePassword(password)) {
            currentUser = user;
            System.out.println(ConsoleColors.GREEN + "Login successful! Welcome, " + user.getName() + ConsoleColors.RESET);

            // Show login details
            System.out.println("\n" + ConsoleColors.CYAN_BOLD + "=== Login Details ===" + ConsoleColors.RESET);
            System.out.println("Name: " + user.getName());
            System.out.println("Email: " + user.getEmail());
            System.out.println("Account Number: " + user.getAccountNumber());
            System.out.println("Balance: $" + df.format(user.getAccount().getBalance()));

        } else {
            System.out.println(ConsoleColors.RED + "Invalid password. Please try again." + ConsoleColors.RESET);
        }
    }

    private static void depositMoney() {
        try {
            double amount = InputValidator.getValidAmount(scanner, "Enter deposit amount: $");
            System.out.print("Enter description: ");
            String description = scanner.nextLine();

            AccountService.deposit(currentUser, amount, description);
        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void withdrawMoney() {
        try {
            double amount = InputValidator.getValidAmount(scanner, "Enter withdrawal amount: $");
            System.out.print("Enter description: ");
            String description = scanner.nextLine();

            AccountService.withdraw(currentUser, amount, description);
        } catch (InvalidInputException | InsufficientBalanceException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void transferMoney() {
        try {
            System.out.print("Enter recipient account number: ");
            String toAccount = scanner.nextLine();

            double amount = InputValidator.getValidAmount(scanner, "Enter transfer amount: $");
            System.out.print("Enter description: ");
            String description = scanner.nextLine();

            AccountService.transfer(currentUser, users, toAccount, amount, description);
        } catch (InvalidInputException | InsufficientBalanceException | InvalidUserException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void addReminder() {
        try {
            System.out.print("Enter bill type: ");
            String billType = scanner.nextLine();

            double amount = InputValidator.getValidAmount(scanner, "Enter bill amount: $");
            LocalDate dueDate = InputValidator.getValidDate(scanner, "Enter due date");

            System.out.print("Enter description: ");
            String description = scanner.nextLine();

            System.out.print("Enter priority (High/Medium/Low): ");
            String priority = scanner.nextLine();

            ReminderService.addReminder(currentUser, billType, amount, dueDate, description, priority);
        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void markReminderAsPaid() {
        try {
            ReminderService.viewReminders(currentUser);
            int index = InputValidator.getValidInt(scanner, "Enter reminder number to mark as paid: ");
            ReminderService.markReminderAsPaid(currentUser, index);
        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void getBudget() {
        try {
            double salary = InputValidator.getValidAmount(scanner, "Enter monthly salary: $");
            double limit = InputValidator.getValidAmount(scanner, "Enter monthly budget limit: $");

            BudgetService.setMonthlyBudget(currentUser, salary, limit);
        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void addGroceryItem() {
        try {
            System.out.print("Enter item name: ");
            String name = scanner.nextLine();

            System.out.print("Enter category: ");

            String category = scanner.nextLine();

            double price = InputValidator.getValidAmount(scanner, "Enter price per unit: $");
            int quantity = InputValidator.getValidInt(scanner, "Enter quantity: ");
            LocalDate purchaseDate = InputValidator.getValidDate(scanner, "Enter purchase date");

            GroceryService.addGroceryItem(currentUser, name, category, price, quantity, purchaseDate);
        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }

    private static void removeGroceryItem() {
        try {
            GroceryService.viewGroceryItems(currentUser);
            int index = InputValidator.getValidInt(scanner, "Enter item number to remove: ");
            currentUser.removeGroceryItem(index - 1);
            System.out.println("Item removed successfully.");
        } catch (InvalidInputException e) {
            System.out.println(ConsoleColors.RED + "Error: " + e.getMessage() + ConsoleColors.RESET);
        }
    }
}