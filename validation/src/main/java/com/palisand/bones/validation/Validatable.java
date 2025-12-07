package com.palisand.bones.validation;

import java.util.List;
import com.palisand.bones.Classes.Property;
import com.palisand.bones.validation.Rules.Violation;

public interface Validatable {
  void doValidate(List<Violation> violations, List<Property<?>> properties) throws Exception;
}
