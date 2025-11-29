import static org.junit.jupiter.api.Assertions.assertTrue;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;
import com.palisand.bones.validation.Max;
import com.palisand.bones.validation.Validator;
import com.palisand.bones.validation.Validator.Violation;
import lombok.Data;

class ValidationsTest {

  @Data
  public static class X {
    @Max(10) private int field = 5;
    private X x = null;
  }

  @Test
  void testValidator() {
    Validator validator = new Validator();
    X x = new X();
    X child = new X();
    child.setField(12);
    x.setX(child);
    List<Violation> result = validator.validate(x);
    System.out.println(result);
    assertTrue(result.size() == 1);
  }

  @Test
  void testMisc() {
    Instant dt = Instant.from(LocalDate.parse("2024-10-29").atStartOfDay(ZoneId.systemDefault()));

    System.out.println(dt);
  }

}
