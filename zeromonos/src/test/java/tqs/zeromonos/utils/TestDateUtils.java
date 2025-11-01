package tqs.zeromonos.utils;

import java.time.DayOfWeek;
import java.time.LocalDate;

/**
 * Utilitários para manipulação de datas nos testes.
 * Centraliza lógica comum de geração de datas válidas e inválidas.
 */
public class TestDateUtils {

    private TestDateUtils() {
        // Classe utilitária - construtor privado
    }

    /**
     * Retorna a próxima data válida para agendamento.
     * - Não pode ser hoje
     * - Não pode ser domingo
     * 
     * @return próxima data válida (amanhã ou depois-amanhã se amanhã for domingo)
     */
    public static LocalDate getNextValidDate() {
        LocalDate date = LocalDate.now().plusDays(1);
        // Se for domingo, pula para segunda
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    /**
     * Retorna uma data válida X dias no futuro.
     * Ajusta automaticamente se cair em domingo.
     * 
     * @param daysInFuture número de dias a partir de hoje
     * @return data válida
     */
    public static LocalDate getValidDateAfterDays(int daysInFuture) {
        LocalDate date = LocalDate.now().plusDays(daysInFuture);
        // Se for domingo, pula para segunda
        if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    /**
     * Retorna o próximo domingo (para testar validação).
     * Usado para verificar que reservas em domingos são rejeitadas.
     * 
     * @return próximo domingo
     */
    public static LocalDate getNextSunday() {
        LocalDate date = LocalDate.now().plusDays(1);
        while (date.getDayOfWeek() != DayOfWeek.SUNDAY) {
            date = date.plusDays(1);
        }
        return date;
    }

    /**
     * Retorna data no passado (para testar validação).
     * 
     * @return data de ontem
     */
    public static LocalDate getPastDate() {
        return LocalDate.now().minusDays(1);
    }

    /**
     * Retorna a data de hoje (para testar validação).
     * Reservas não podem ser feitas para o mesmo dia.
     * 
     * @return data de hoje
     */
    public static LocalDate getToday() {
        return LocalDate.now();
    }

    /**
     * Verifica se uma data é válida para agendamento.
     * 
     * @param date data a verificar
     * @return true se a data é válida (não é hoje, não é passado, não é domingo)
     */
    public static boolean isValidBookingDate(LocalDate date) {
        if (date == null) {
            return false;
        }
        
        LocalDate today = LocalDate.now();
        
        // Não pode ser hoje ou no passado
        if (!date.isAfter(today)) {
            return false;
        }
        
        // Não pode ser domingo
        return date.getDayOfWeek() != DayOfWeek.SUNDAY;
    }
}
