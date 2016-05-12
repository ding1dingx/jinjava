package com.hubspot.jinjava.el.ext;

import com.hubspot.jinjava.objects.collections.PyList;
import de.odysseus.el.misc.LocalMessages;
import de.odysseus.el.tree.Bindings;
import de.odysseus.el.tree.impl.ast.AstBracket;
import de.odysseus.el.tree.impl.ast.AstNode;

import javax.el.ELContext;
import javax.el.ELException;
import javax.el.PropertyNotFoundException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Iterator;

public class AstRangeBracket extends AstBracket {

  protected final AstNode rangeMax;

  public AstRangeBracket(AstNode base, AstNode rangeStart, AstNode rangeMax, boolean lvalue, boolean strict, boolean ignoreReturnType) {
    super(base, rangeStart, lvalue, strict, ignoreReturnType);
    this.rangeMax = rangeMax;
  }

  @Override
  public Object eval(Bindings bindings, ELContext context) {
    Object base = prefix.eval(bindings, context);
    if (base == null) {
      throw new PropertyNotFoundException(LocalMessages.get("error.property.base.null", prefix));
    }
    boolean baseIsString = base.getClass().equals(String.class);
    if (!Iterable.class.isAssignableFrom(base.getClass()) && !base.getClass().isArray() && !baseIsString) {
      throw new ELException("Property " + prefix + " is not a sequence.");
    }

    Object start = property.eval(bindings, context);
    if (start == null && strict) {
      return Collections.emptyList();
    }
    if (!(start instanceof Number)) {
      throw new ELException("Range start is not a number");
    }

    Object end = rangeMax.eval(bindings, context);
    if (end == null && strict) {
      return Collections.emptyList();
    }
    if (!(end instanceof Number)) {
      throw new ELException("Range end is not a number");
    }

    int startNum = ((Number) start).intValue();
    int endNum = ((Number) end).intValue();

    // https://github.com/HubSpot/jinjava/issues/52
    if (baseIsString) {
      return evalString((String) base, startNum, endNum);
    }

    Iterable<?> baseItr;

    if (base.getClass().isArray()) {
      baseItr = Arrays.asList((Object[]) base);
    }
    else {
      baseItr = (Iterable<?>) base;
    }

    PyList result = new PyList(new ArrayList<>());
    int index = 0;

    Iterator<?> baseIterator = baseItr.iterator();
    while (baseIterator.hasNext()) {
      Object next = baseIterator.next();

      if (index >= startNum) {
        if (index >= endNum) {
          break;
        }
        result.add(next);
      }
      index++;
    }

    return result;
  }

  private String evalString(String base, int startNum, int endNum) {
    if (startNum > endNum) {
      return "";
    }
    if (startNum <= 0) {
      startNum = 0;
    }
    if (endNum > base.length()) {
      endNum = base.length();
    }
    return base.substring(startNum, endNum);
  }

  @Override
  public String toString() {
    return "[:]";
  }

}
