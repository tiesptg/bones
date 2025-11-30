import static org.junit.jupiter.api.Assertions.assertTrue;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.util.List;
import java.util.function.Predicate;
import org.junit.jupiter.api.Test;
import com.palisand.bones.validation.Max;
import com.palisand.bones.validation.NotNull;
import com.palisand.bones.validation.ValidWhen;
import com.palisand.bones.validation.Validator;
import com.palisand.bones.validation.Validator.Violation;
import lombok.Data;

class ValidationsTest {

  @Data
  public static class X {

    public static class XTest implements Predicate<X> {
      @Override
      public boolean test(ValidationsTest.X x) {
        return x.field <= 5;
      }
    }

    @Max(10) private int field = 5;
    @ValidWhen(XTest.class)
    @NotNull private X x = null;
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

  @Retention(RetentionPolicy.RUNTIME)
  public @interface Trial {
    Class<? extends Predicate<?>> test();
  }

  public static class TestVictim implements Predicate<Victim> {

    @Override
    public boolean test(ValidationsTest.Victim victim) {
      return victim != null;
    }
  }

  @Trial(test = TestVictim.class)
  public static class Victim {

  }

  @Test
  void testMisc() throws Exception {
    Validator validator = new Validator();
    X x = new X();
    x.setField(8);
    List<Violation> violations = validator.validate(x);
    assertTrue(violations.isEmpty());
    x.setField(0);
    violations = validator.validate(x);
    assertTrue(violations.size() == 1);
    x.x = new X();
    x.x.setField(8);
    violations = validator.validate(x);
    assertTrue(violations.isEmpty());
  }

}
