/*
 * The MIT License
 *
 * Copyright 2019 Intuit Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.intuit.karate.driver;

import java.util.Map;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 *
 * @author pthomas3
 */
public class Finder {

    private static final Logger LOGGER = LoggerFactory.getLogger(Finder.class);

    public static enum Type {
        RIGHT,
        LEFT,
        ABOVE,
        BELOW,
        NEAR
    }

    private final Driver driver;
    private final String fromLocator;
    private final Type type;

    private String tag = "INPUT";

    public Finder(Driver driver, String fromLocator, Type type) {
        this.driver = driver;
        this.fromLocator = fromLocator;
        this.type = type;
    }

    private static String forLoopChunk(Finder.Type type) {
        switch (type) {
            case RIGHT:
                return "x += s;";
            case BELOW:
                return "y += s;";
            case LEFT:
                return "x -= s;";
            case ABOVE:
                return "y -= s;";
            default: // NEAR
                return " var a = 0.381966 * i; var x = (s + a) * Math.cos(a); var y = (s + a) * Math.sin(a);";
        }
    }

    private static String findScript(Driver driver, String locator, Finder.Type type, String findTag) {
        Map<String, Object> pos = driver.position(locator);
        Number xNum = (Number) pos.get("x");
        Number yNum = (Number) pos.get("y");
        Number width = (Number) pos.get("width");
        Number height = (Number) pos.get("height");
        // get center point
        int x = xNum.intValue() + width.intValue() / 2;
        int y = yNum.intValue() + height.intValue() / 2;
        // o: origin, a: angle, s: step
        String fun = "var gen = " + DriverOptions.KARATE_REF_GENERATOR + ";"
                + " var o = { x: " + x + ", y: " + y + "}; var s = 10; var x = 0; var y = 0;"
                + " for (var i = 0; i < 300; i++) {"
                + forLoopChunk(type)
                + " var e = document.elementFromPoint(o.x + x, o.y + y);"
                + " console.log(o.x +':' + o.y, x + ':' + y, e);"
                + " if (e && e.tagName == '" + findTag.toUpperCase() + "') return gen(e); "
                + " } return null";
        return DriverOptions.wrapInFunctionInvoke(fun);
    }

    private String getDebugString() {
        return fromLocator + ", " + type + ", " + tag;
    }

    public Element find() {
        String js = findScript(driver, fromLocator, type, tag);
        String karateRef = (String) driver.script(js);
        if (karateRef == null) {
            LOGGER.warn("friendly locator failed will try once more after 500ms: {}", getDebugString());
            driver.getOptions().sleep(); // special case
            karateRef = (String) driver.script(js);
            if (karateRef == null) {
                throw new RuntimeException("unable to find: " + getDebugString());
            }
        }
        return DriverElement.locatorExists(driver, DriverOptions.karateLocator(karateRef));
    }

    public Element find(String tag) {
        this.tag = tag;
        return find();
    }

    public Element clear() {
        return find().clear();
    }

    public Element input(String value) {
        return find().input(value);
    }

    public Element click() {
        return find().click();
    }

}