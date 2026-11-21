// 5.1 Исправленный вариант
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class ShiftPlanFixed {

    private static int failedChecks = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    
    // Замечание 1: Потокобезопасный список
    private static final List<String> log = Collections.synchronizedList(new ArrayList<>());

    // Замечание 4: Вынос магических чисел в константы
    private static final double MAX_SHIFT_HOURS = 10.0;
    private static final double EPSILON = 1e-9;

    public static void main(String[] args) {
        List<Brigade> brigades = new ArrayList<>();
        brigades.add(new Brigade("Иванов", 11.0, true));
        brigades.add(new Brigade("Петров", 0.0, true));
        brigades.add(new Brigade("Сидоров", 4.5, false));
        brigades.add(new Brigade("Козлов", 8.0, true));

        System.out.println("--- Самопроверки исправленной программы ---");
        
        String result = make(brigades, 2);
        check("Наряд сформирован корректно", result.equals("Петров; Козлов; "));
        check("Исходный список не изменился", brigades.size() == 4);

        boolean caughtShortage = false;
        try {
            make(brigades, 10);
        } catch (IllegalArgumentException e) {
            caughtShortage = true;
        }
        check("Выброшено исключение при нехватке бригад", caughtShortage);

        boolean caughtNegative = false;
        try {
            make(brigades, -1);
        } catch (IllegalArgumentException e) {
            caughtNegative = true;
        }
        check("Выброшено исключение при отрицательном количестве", caughtNegative);

        check("Журнал содержит запись о формировании", !log.isEmpty());

        if (failedChecks > 0) {
            throw new AssertionError("не пройдено проверок: " + failedChecks);
        }
        System.out.println("Все проверки пройдены.");
    }

    // Замечание 2: Разделение ответственности
    public static String make(List<Brigade> brigades, int requiredBrigadesCount) {
        String result = buildShiftPlan(brigades, requiredBrigadesCount);
        saveToFile(result);
        log.add(LocalDate.now().format(DATE_FORMATTER) + " наряд сформирован");
        return result;
    }

    public static String buildShiftPlan(List<Brigade> brigades, int requiredBrigadesCount) {
        // Замечание 3: Проверка на null и <= 0
        if (requiredBrigadesCount <= 0 || brigades == null) {
            throw new IllegalArgumentException("Некорректные входные данные: количество должно быть > 0, список не null");
        }

        List<Brigade> filteredBrigades = new ArrayList<>();
        for (Brigade brigade : brigades) {
            boolean isOverworked = brigade.hoursPerShift > MAX_SHIFT_HOURS + EPSILON;
            if (isOverworked || !brigade.admitted) {
                continue;
            }
            filteredBrigades.add(brigade);
        }

        if (filteredBrigades.size() < requiredBrigadesCount) {
            throw new IllegalArgumentException("Недостаточно доступных бригад: запрошено " 
                    + requiredBrigadesCount + ", доступно " + filteredBrigades.size());
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < requiredBrigadesCount; i++) {
            result.append(filteredBrigades.get(i).lastName).append("; ");
        }
        return result.toString();
    }

    private static void saveToFile(String result) {
        try (FileWriter writer = new FileWriter("наряд.txt")) {
            writer.write(result);
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи в файл", e);
        }
    }

    static void check(String checkName, boolean passed) {
        System.out.println((passed ? "[OK]   " : "[FAIL] ") + checkName);
        if (!passed) {
            failedChecks++;
        }
    }
}

// Замечание 5: Инкапсуляция и валидация
class Brigade {
    private final String lastName;
    private final double hoursPerShift;
    private final boolean admitted;

    Brigade(String lastName, double hoursPerShift, boolean admitted) {
        if (lastName == null || lastName.isBlank()) {
            throw new IllegalArgumentException("Фамилия не может быть пустой");
        }
        if (hoursPerShift < 0) {
            throw new IllegalArgumentException("Часы не могут быть отрицательными");
        }
        this.lastName = lastName;
        this.hoursPerShift = hoursPerShift;
        this.admitted = admitted;
    }
}
