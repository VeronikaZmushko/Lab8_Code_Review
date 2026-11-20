// 5.1 Исправленный вариант
import java.io.FileWriter;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

public class ShiftPlanFixed {

    private static int failedChecks = 0;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final List<String> log = new ArrayList<>();

    public static void main(String[] args) {
        List<Brigade> brigades = new ArrayList<>();
        brigades.add(new Brigade("Иванов", 11.0, true));
        brigades.add(new Brigade("Петров", 0.0, true));
        brigades.add(new Brigade("Сидоров", 4.5, false));
        brigades.add(new Brigade("Козлов", 8.0, true));

        System.out.println("--- Самопроверки исправленной программы ---");
        
        // 1. Успешное формирование наряда (Иванов отсеян из-за 11 часов, Сидоров - из-за недопуска)
        String result = make(brigades, 2);
        check("Наряд сформирован корректно", result.equals("Петров; Козлов; "));
        
        // 2. Исходный список не изменился (исправлен побочный эффект)
        check("Исходный список не изменился", brigades.size() == 4);

        // 3. Исключение при нехватке бригад
        boolean caughtShortage = false;
        try {
            make(brigades, 10);
        } catch (IllegalArgumentException e) {
            caughtShortage = true;
        }
        check("Выброшено исключение при нехватке бригад", caughtShortage);

        // 4. Исключение при отрицательном запросе
        boolean caughtNegative = false;
        try {
            make(brigades, -1);
        } catch (IllegalArgumentException e) {
            caughtNegative = true;
        }
        check("Выброшено исключение при отрицательном количестве", caughtNegative);

        // 5. Проверка записи в журнал
        check("Журнал содержит запись о формировании", !log.isEmpty());

        if (failedChecks > 0) {
            throw new AssertionError("не пройдено проверок: " + failedChecks);
        }
        System.out.println("Все проверки пройдены.");
    }

    public static String make(List<Brigade> brigades, int requiredBrigadesCount) {
        if (requiredBrigadesCount < 0) {
            throw new IllegalArgumentException("Количество бригад не может быть отрицательным");
        }

        // Исправление 1, 6, 8, 9: Создаем новый список, не изменяем исходный.
        List<Brigade> filteredBrigades = new ArrayList<>();
        for (Brigade brigade : brigades) {
            boolean isOverworked = brigade.hoursPerShift > 10.0 + 1e-9;
            if (isOverworked || !brigade.admitted) {
                continue;
            }
            filteredBrigades.add(brigade);
        }

        // Исправление 2, 4: Проверка на нехватку и пустой список
        if (filteredBrigades.size() < requiredBrigadesCount) {
            throw new IllegalArgumentException("Недостаточно доступных бригад: запрошено " 
                    + requiredBrigadesCount + ", доступно " + filteredBrigades.size());
        }

        StringBuilder result = new StringBuilder();
        for (int i = 0; i < requiredBrigadesCount; i++) {
            result.append(filteredBrigades.get(i).lastName).append("; ");
        }

        // Исправление 3: try-with-resources и проброс исключения
        try (FileWriter writer = new FileWriter("наряд.txt")) {
            writer.write(result.toString());
        } catch (IOException e) {
            throw new RuntimeException("Ошибка при записи в файл", e);
        }

        // Исправление 5: Потокобезопасный DateTimeFormatter
        log.add(LocalDate.now().format(DATE_FORMATTER) + " наряд сформирован");
        return result.toString();
    }

    static void check(String checkName, boolean passed) {
        System.out.println((passed ? "[OK]   " : "[FAIL] ") + checkName);
        if (!passed) {
            failedChecks++;
        }
    }
}

class Brigade {
    String lastName;
    double hoursPerShift;
    boolean admitted;

    Brigade(String lastName, double hoursPerShift, boolean admitted) {
        this.lastName = lastName;
        this.hoursPerShift = hoursPerShift;
        this.admitted = admitted;
    }
}
