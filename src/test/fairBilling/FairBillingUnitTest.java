package fairBilling;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import static fairBilling.Action.End;
import static fairBilling.Action.Start;
import static org.junit.jupiter.api.Assertions.*;

public class FairBillingUnitTest {

    private final PrintStream systemOut = System.out;
    private final ByteArrayOutputStream outContent = new ByteArrayOutputStream();

    @BeforeEach
    public void setUpStreams() throws NoSuchFieldException, IllegalAccessException {
        Field lastTime = FairBilling.class.getDeclaredField("lastTime");
        Field firstTime = FairBilling.class.getDeclaredField("firstTime");

        lastTime.setAccessible(true);
        lastTime.set(null, null);
        firstTime.setAccessible(true);
        firstTime.set(null, null);
        System.setOut(new PrintStream(outContent));
    }

    @AfterEach
    public void restoreStreams() {
        System.setOut(systemOut);
    }


    @Test
    void main_shouldWriteOutTheUsernameSessionsAndNumberOfSeconds() throws IOException {
        FairBilling.main(new String[] {"src/test/resources/singleExample.txt"});
        assertEquals("ALICE99 1 0\n", outContent.toString());
    }


    @Test
    void main_shouldWriteOutTheExpectedConsoleOutput_FromTheGivenExample() throws IOException {

        FairBilling.main(new String[] {"src/test/resources/givenExample.txt"});
        assertEquals("ALICE99 4 240\nCHARLIE 3 37\n", outContent.toString());
    }

    @Test
    void generateBill_ShouldAddABillForUsername_WhenEntriesAreSupplied() {
        List<LogEntry> entries = new ArrayList<>();
        LocalTime now = LocalTime.now();
        LocalTime future = now.plusSeconds(10);
        entries.add(new LogEntry(now, Start));
        entries.add(new LogEntry(future, End));
        ConcurrentMap<String, Bill> fairBills = new ConcurrentHashMap<>();

        FairBilling.generateBill("AUser", entries, fairBills);

        assertEquals(1, fairBills.keySet().size());
        Bill bill = fairBills.get("AUser");
        assertEquals(10, bill.getTotalTimeSeconds());
    }


    @Test
    void generateBill_ShouldUseTheLatestTimeStamp_WhenAStartActionHasNoCorrespondingEnd() throws NoSuchFieldException, IllegalAccessException {
        List<LogEntry> entries = new ArrayList<>();
        LocalTime now = LocalTime.now();
        LocalTime future = now.plusSeconds(10);
        entries.add(new LogEntry(now, Start));
        ConcurrentMap<String, Bill> fairBills = new ConcurrentHashMap<>();

        Field lastTime = FairBilling.class.getDeclaredField("lastTime");
        lastTime.setAccessible(true);
        lastTime.set(null, future);
        FairBilling.generateBill("AUser", entries, fairBills);

        assertEquals(1, fairBills.keySet().size());
        Bill bill = fairBills.get("AUser");
        assertEquals(10, bill.getTotalTimeSeconds());
    }

    @Test
    void generateBill_ShouldUseTheFirstTimeStamp_WhenAEndActionHasNoCorrespondingStart() throws NoSuchFieldException, IllegalAccessException {
        List<LogEntry> entries = new ArrayList<>();
        LocalTime now = LocalTime.now();
        LocalTime future = now.plusSeconds(10);
        entries.add(new LogEntry(future, End));
        ConcurrentMap<String, Bill> fairBills = new ConcurrentHashMap<>();

        Field firstTime = FairBilling.class.getDeclaredField("firstTime");
        firstTime.setAccessible(true);
        firstTime.set(null, now);
        FairBilling.generateBill("AUser", entries, fairBills);

        assertEquals(1, fairBills.keySet().size());
        Bill bill = fairBills.get("AUser");
        assertEquals(10, bill.getTotalTimeSeconds());
    }

    @Test
    void validEntryFormat_shouldReturnTrue_WhenInputIsThreeParts_WithValidTimeStamp_AndValidUsername_andValidAction() {
        String entry = "12:00:00 CHARLIE Start";
        assertTrue(FairBilling.validEntryFormat(entry));
    }


    @Test
    void validEntryFormat_shouldReturnFalse_WhenTimeStampIsInvalid() {
        String entry = "99:99:99 CHARLIE Start";
        assertFalse(FairBilling.validEntryFormat(entry));
    }

    @Test
    void validEntryFormat_shouldReturnFalse_WhenActionIsNotValid() {
        String entry = "12:12:00 CHARLIE Begin";
        assertFalse(FairBilling.validEntryFormat(entry));
    }

    @Test
    void validEntryFormat_shouldReturnFalse_WhenUserNameHasSpecialChars() {
        String entry = "12:12:00 #$%R^I&* Start";
        assertFalse(FairBilling.validEntryFormat(entry));
    }

    @Test
    void validEntryFormat_shouldReturnFalse_WhenEntryHasLessThanThreeParts() {
        String entry = "12:12:00 CHARLIE";
        assertFalse(FairBilling.validEntryFormat(entry));
    }

    @Test
    void validEntryFormat_shouldReturnFalse_WhenEntryHasMoreThanThreeParts() {
        String entry = "12:00:00 CHARLIE Start AndOtherThings";
        assertFalse(FairBilling.validEntryFormat(entry));
    }
}
