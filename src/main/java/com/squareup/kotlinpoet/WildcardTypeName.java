/*
 * Copyright (C) 2015 Square, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.squareup.kotlinpoet;

import java.io.IOException;
import java.lang.reflect.Type;
import java.lang.reflect.WildcardType;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.lang.model.element.TypeParameterElement;
import javax.lang.model.type.TypeMirror;
import kotlin.reflect.KClass;

import static com.squareup.kotlinpoet.Util.checkArgument;

public final class WildcardTypeName extends TypeName {
  public final List<TypeName> upperBounds;
  public final List<TypeName> lowerBounds;

  private WildcardTypeName(List<TypeName> upperBounds, List<TypeName> lowerBounds) {
    this(upperBounds, lowerBounds, new ArrayList<AnnotationSpec>());
  }

  private WildcardTypeName(List<TypeName> upperBounds, List<TypeName> lowerBounds,
      List<AnnotationSpec> annotations) {
    super(annotations);
    this.upperBounds = Util.immutableList(upperBounds);
    this.lowerBounds = Util.immutableList(lowerBounds);

    checkArgument(this.upperBounds.size() == 1, "unexpected extends bounds: %s", upperBounds);
  }

  @Override public WildcardTypeName annotated(List<AnnotationSpec> annotations) {
    return new WildcardTypeName(upperBounds, lowerBounds, concatAnnotations(annotations));
  }

  @Override public TypeName withoutAnnotations() {
    return new WildcardTypeName(upperBounds, lowerBounds);
  }

  @Override CodeWriter emit(CodeWriter out) throws IOException {
    if (lowerBounds.size() == 1) {
      return out.emit("in $T", lowerBounds.get(0));
    }
    return upperBounds.get(0).equals(ANY)
        ? out.emit("*")
        : out.emit("out $T", upperBounds.get(0));
  }

  /**
   * Returns a type that represents an unknown type that extends {@code bound}. For example, if
   * {@code bound} is {@code CharSequence.class}, this returns {@code ? extends CharSequence}. If
   * {@code bound} is {@code Object.class}, this returns {@code ?}, which is shorthand for {@code
   * ? extends Object}.
   */
  public static WildcardTypeName subtypeOf(TypeName upperBound) {
    return new WildcardTypeName(Collections.singletonList(upperBound),
        Collections.<TypeName>emptyList());
  }

  public static WildcardTypeName subtypeOf(Type upperBound) {
    return subtypeOf(TypeName.get(upperBound));
  }

  public static WildcardTypeName subtypeOf(KClass<?> upperBound) {
    return subtypeOf(TypeName.get(upperBound));
  }

  /**
   * Returns a type that represents an unknown supertype of {@code bound}. For example, if {@code
   * bound} is {@code String.class}, this returns {@code ? super String}.
   */
  public static WildcardTypeName supertypeOf(TypeName lowerBound) {
    return new WildcardTypeName(Collections.<TypeName>singletonList(ANY),
        Collections.singletonList(lowerBound));
  }

  public static WildcardTypeName supertypeOf(Type lowerBound) {
    return supertypeOf(TypeName.get(lowerBound));
  }

  public static WildcardTypeName supertypeOf(KClass<?> lowerBound) {
    return supertypeOf(TypeName.get(lowerBound));
  }

  public static TypeName get(javax.lang.model.type.WildcardType mirror) {
    return get(mirror, new LinkedHashMap<TypeParameterElement, TypeVariableName>());
  }

  static TypeName get(
      javax.lang.model.type.WildcardType mirror,
      Map<TypeParameterElement, TypeVariableName> typeVariables) {
    TypeMirror extendsBound = mirror.getExtendsBound();
    if (extendsBound == null) {
      TypeMirror superBound = mirror.getSuperBound();
      if (superBound == null) {
        return subtypeOf(Object.class);
      } else {
        return supertypeOf(TypeName.get(superBound, typeVariables));
      }
    } else {
      return subtypeOf(TypeName.get(extendsBound, typeVariables));
    }
  }

  public static TypeName get(WildcardType wildcardName) {
    return get(wildcardName, new LinkedHashMap<Type, TypeVariableName>());
  }

  static TypeName get(WildcardType wildcardName, Map<Type, TypeVariableName> map) {
    return new WildcardTypeName(
        list(wildcardName.getUpperBounds(), map),
        list(wildcardName.getLowerBounds(), map));
  }
}
