/*-
 * Mailific SMTP Server Library
 *
 * Copyright (C) 2023 Joe Humphreys
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.mailific.spf.policy;

import net.mailific.spf.ResultCode;

public enum Qualifier {
  PASS("+", ResultCode.Pass),
  FAIL("-", ResultCode.Fail),
  SOFT_FAIL("~", ResultCode.Softfail),
  NEUTRAL("?", ResultCode.Neutral);

  private final String symbol;
  private final ResultCode resultCode;

  private Qualifier(String symbol, ResultCode resultCode) {
    this.symbol = symbol;
    this.resultCode = resultCode;
  }

  public String getSymbol() {
    return symbol;
  }

  public static Qualifier fromSymbol(String s) {
    switch (s) {
      case "+":
        return PASS;
      case "-":
        return FAIL;
      case "?":
        return NEUTRAL;
      case "~":
        return SOFT_FAIL;
      default:
        throw new IllegalArgumentException("No such qualifier: " + s);
    }
  }

  public ResultCode getResultCode() {
    return resultCode;
  }
}
