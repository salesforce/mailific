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

package net.mailific.spf.main;

import java.net.InetAddress;
import java.util.stream.Stream;
import net.mailific.spf.dns.JndiResolver;

public class Main {

  public static void main(String[] args) {
    try {
      JndiResolver resolver = new JndiResolver();
      switch (args[0].toUpperCase()) {
        case "TXT":
          System.out.println(resolver.resolveTxtRecords(args[1]));
          break;
        case "A":
          System.out.println(resolver.resolveARecords(args[1]));
          break;
        case "AAAA":
          System.out.println(resolver.resolveAAAARecords(args[1]));
          break;
        case "MX":
          System.out.println(resolver.resolveMXRecords(args[1]));
          break;
        case "PTR":
          System.out.println(resolver.resolvePtrRecords(args[1]));
          break;
      }
    } catch (Exception e) {
      e.printStackTrace();
    }
  }

  public static void xmain(String[] args) {
    if (args == null
        || Stream.of(args).anyMatch(s -> s.equals("-h"))
        || args.length < 2
        || args.length > 3) {
      System.out.println("Usage: java -jar <path to jar> <ip> <sender>");
      System.exit(1);
    }

    try {
      InetAddress ip = InetAddress.getByName(args[0]);
      String sender = args[1];

    } catch (Exception e) {
      e.printStackTrace();
    }
  }
}
