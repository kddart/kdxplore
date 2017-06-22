/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016,2017  Diversity Arrays Technology, Pty Ltd.
    
    KDXplore may be redistributed and may be modified under the terms
    of the GNU General Public License as published by the Free Software
    Foundation, either version 3 of the License, or (at your option)
    any later version.
    
    KDXplore is distributed in the hope that it will be useful,
    but WITHOUT ANY WARRANTY; without even the implied warranty of
    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
    GNU General Public License for more details.
    
    You should have received a copy of the GNU General Public License
    along with KDXplore.  If not, see <http://www.gnu.org/licenses/>.
*/
package com.diversityarrays.kdcompute.db.helper;

import java.text.DecimalFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.script.ScriptContext;
import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;

import com.diversityarrays.kdcompute.db.KnobDataType;
import com.diversityarrays.kdcompute.db.helper.RangeParser.RangeParseResult;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.Pair;

/**
 * Accepted rules are:
 * <p>
 * <b>CHOICE</b>:
 * <br>
 * A number of "pipe"-seeparated values; e.g. <code><b>value_1|value_2|value_3</b></code>
 * <br>
 * The {@link ChoicesValueEditor} will use<ul>
 *   <li>a Checkbox for a single "choice" (where unchecked means "")</li>
 *   <li>two RadioButtons for two choices</li>
 *   <li>a ComboBox for more than two choices</li>
 * </ul>
 * 
 * <p>
 * <b>Range</b>
 * <br>
 * Accepts a number of variations:<ul>
 *   <li>Groovy style ranges (e.g. <code><b>34 ..&lt; 50</code>)</b></li>
 *   <li>Maths-like parenthesized ranges:  <code><b>[ 34 .. 50 )</b></code></li>
 *   <li>Verious "BOOLEX" expressions:
 *     <ul>
 *       <li>"variable in the middle":  <code><b>34 &lt;= x &lt; 50</b></code></li>
 *       <li>"two inequalities":  <code><b>x &gt= 34 &amp;&amp; x &lt; 50</b></code></li>
 *       <li>"single inequality":  <code><b>x &gt; 33</b></code> (i.e. minimum is 34)</li>
 *     </ul>
 *   </li>
 * </ul>
 * 
 * <p>
 * <b>DateRule</b>
 * <br>NO LONGER SUPPORTED
 * 
 * <p>
 * <b>Text</b>
 * <br>
 * Basically - accept anything the user types.
 * @see {@link ChoicesValueEditor}
 * @author brianp
 *
 */
@SuppressWarnings("nls")
public abstract class KnobValidationRule implements Predicate<String> {

    static public Pair<KnobDataType,KnobValidationRule> create(String text) 
    throws InvalidRuleException 
    {
        String input = text==null ? "" : text;

//        if ("date".equalsIgnoreCase(input)) {
//            return new Pair<>(KnobDataType.DATE, new DateRule());
//        }
        
        if ("text".equalsIgnoreCase(input) || "regex(.*)".equalsIgnoreCase(input)) {
            return new Pair<>(KnobDataType.TEXT, new TextRule());
        }

        if (input.startsWith("javascript:")) {
            return new Pair<>(KnobDataType.FORMULA, 
                    new JavaScriptRule(input.substring(11)));
        }
        
        // MIN  .. MAX
        // MIN <..  MAX
        // MIN <..< MAX
        // MIN  ..< MAX
        // [ MIN .. MAX ]
        // ( MIN .. MAX ]
        // ( MIN .. MAX )
        // [ MIN .. MAX )
        
        int dotsPos = input.indexOf("..");
        if (dotsPos >= 0) {
            if (input.startsWith("..")) {
                throw new InvalidRuleException("Missing range start in: " + input);
            }
            if (input.endsWith("..")) {
                throw new InvalidRuleException("Missing range end in: " + input);
            }
//            if (! Character.isDigit(input.charAt(0))) {
//                throw new InvalidRuleException("Non-numeric range start: " + input.substring(0, dotsPos));
//            }
//            if (! Character.isDigit(input.charAt(input.length() - 1))) {
//                throw new InvalidRuleException("Non-numeric range end: " + input.substring(dotsPos + 2));
//            }
            
            RangeParseResult rpr = RangeParser.parse(input, ValidationRuleType.RANGE);
            if (rpr == null) {
                throw new InvalidRuleException(input);
            }
            
            Range range = new Range(rpr.validationRuleType.includesLowerLimit(),
                    rpr.validationRuleType.includesUpperLimit(),
                    rpr.computeDecimalPlaces(), 
                    rpr.lowerLimit_, rpr.upperLimit_);

            if (range.nDecimalPlaces > 0) {
                return new Pair<>(KnobDataType.DECIMAL, range);
            }
            return new Pair<>(KnobDataType.INTEGER, range);
        }
        else if (input.toLowerCase().startsWith("boolex(") && input.endsWith(")")) {
            String middle = input.substring(7, input.length()-1);

            RangeParseResult rpr = RangeParser.parse(middle, ValidationRuleType.RANGE);
            if (rpr == null) {
                throw new InvalidRuleException(input);
            }
            Range range = new Range(rpr.validationRuleType.includesLowerLimit(),
                    rpr.validationRuleType.includesUpperLimit(),
                    rpr.computeDecimalPlaces(), 
                    rpr.lowerLimit_, rpr.upperLimit_);

            if (range.nDecimalPlaces > 0) {
                return new Pair<>(KnobDataType.DECIMAL, range);
            }
            return new Pair<>(KnobDataType.INTEGER, range);
        }

        if (! input.isEmpty()) {

            List<String> choices = new ArrayList<>();
            for (String part : input.split("\\|")) {
                part = part.trim();
                if (! part.isEmpty()) {
                    choices.add(part);
                }
            }
            
            // NOTE: we allow choices to be of size 1
            if (choices.size() > 0) {
                return new Pair<>(KnobDataType.CHOICE, new Choice(choices));
            }
        }

        throw new InvalidRuleException("Not a valid rule: '" + input + "'");
    }
    
    public KnobValidationRule() { }
    
    abstract public String getExpression();
    
    
//    @SuppressWarnings("unused")
//    @Deprecated
//    private static Pair<KnobDataType, KnobValidationRule> parseRange(int dotsPos, String input) throws InvalidRuleException {
//
//        // Try LOW..HI
//        
//        double nBefore;
//        double nAfter;
//        String before = input.substring(0, dotsPos);
//        String after = input.substring(dotsPos+2);
//
//        try {
//            nBefore = Double.parseDouble(before);
//            nAfter = Double.parseDouble(after);
//            
//            return makeRange(input, true, true, before, after, nBefore, nAfter);   
//        }
//        catch (NumberFormatException ignore) {
//            
//        }
//        
//        // Need at least a..b
//        Pattern parensPattern = Pattern.compile("^[\\[(].{4,}[\\])]$");
//        
//        if (parensPattern.matcher(input).matches()) {
//            char ch_1 = input.charAt(0);
//            char ch_n = input.charAt(input.length()-1);
//
//            boolean includeLower = ch_1 == '[';
//            boolean includeUpper = ch_n == ']';
//
//            before = input.substring(1, dotsPos);
//            after = input.substring(dotsPos+2, input.length()-2);
//            
//            return makeRange(input, includeLower, includeUpper, before, after);
//        }
//
//        boolean includeLower = true;
//        if (before.endsWith("<")) {
//            includeLower = false;
//            before = before.substring(0,  before.length()-1);
//        }
//        
//        boolean includeUpper = true;
//        if (after.startsWith("<")) {
//            includeUpper = false;
//            after = after.substring(1);
//        }
//        
//        return makeRange(input, includeLower, includeUpper, before, after);
//    }
    
    

//    private static Pair<KnobDataType, KnobValidationRule> makeRange(
//            String input,
//            boolean includeLower, boolean includeUpper, 
//            String before, String after) throws InvalidRuleException 
//    {
//        
//        double nBefore;
//        double nAfter;
//        try {
//            nBefore = Double.parseDouble(before);
//        }
//        catch (NumberFormatException e) {
//            throw new InvalidRuleException("bad first number: " + before);
//        }
//        try {
//            nAfter = Double.parseDouble(after);
//        }
//        catch (NumberFormatException e) {
//            throw new InvalidRuleException("bad second number: " + after);
//        }
//        return makeRange(input,
//                includeLower, includeUpper,
//                before, after,
//                nBefore, nAfter);
//    }
    
//    private static Pair<KnobDataType, KnobValidationRule> makeRange(
//            String input,
//            boolean includeLower, boolean includeUpper, 
//            String before, String after,
//            double nBefore, double nAfter) throws InvalidRuleException 
//    {
//        if (nBefore > nAfter) {
//            throw new InvalidRuleException("first > second: " + input);
//        }
//        
//        int nPlacesBefore = 0;
//        int nPlacesAfter = 0;
//        int dotPos = before.lastIndexOf('.');
//        if (dotPos >= 0) {
//            nPlacesBefore = before.substring(dotPos+1).length();
//        }
//        dotPos = after.lastIndexOf('.');
//        if (dotPos >= 0) {
//            nPlacesAfter = after.substring(dotPos+1).length();
//        }
//        
//        int nDecs = Math.max(nPlacesBefore, nPlacesAfter);
//        
//        KnobDataType type = nDecs <= 0 ? KnobDataType.INTEGER : KnobDataType.DECIMAL;
//        return new Pair<>(type,
//                new Range(includeLower, includeUpper, nDecs, new double[] {nBefore, nAfter}));
//    }
    
    static public class TextRule extends KnobValidationRule {

        @Override
        public boolean test(String t) {
            return true;
        }

        @Override
        public String getExpression() {
            return "text";
        }
        
    }
    
    @Deprecated
    static public class DateRule extends KnobValidationRule {
        
        static final String[] VARIANTS = {
                "yyyy/MM/dd",
                "yyyy-MM-dd",
                "yyyy/MMM/dd",
                "yyyy-MMM-dd"
        };

        @Override
        public String getExpression() {
            return "date";
        }

        @Override
        public boolean test(String t) {
            Date date = null;
            for (String variant : VARIANTS) {
                try {
                    date = new SimpleDateFormat(variant).parse(t);
                    break;
                }
                catch (ParseException e) {}
            }
            return date != null;
        }
        
    }

    static public class Range extends KnobValidationRule {
        
        public static final int MAX_DECIMAL_NDECS = 6;

        private final boolean includeLower;
        private final boolean includeUpper;
        private final Double[] limits_;
        private final Double[] inclusiveLimits;
        private final double increment;
        private final String numberFormat;
        private final DecimalFormat decimalFormat;
        private final int nDecimalPlaces;
        private final String expression;
        
        public Range(int nDecs) {
            // MUST use true,true else we get a NPE
            this(true, true, nDecs, null, null);
        }
        
        Range(boolean includeLower, boolean includeUpper, int nDecs, 
                Double lowerLimit, Double upperLimit) {
            this.includeLower = includeLower;
            this.includeUpper = includeUpper;
            this.nDecimalPlaces = nDecs;
            this.limits_ = new Double[] { lowerLimit, upperLimit };
            
            if (nDecimalPlaces <= 0) {
                increment = 1;
                numberFormat = "#";
            }
            else {
                increment = Math.pow(10.0, -nDecimalPlaces);
                StringBuilder sb = new StringBuilder("0.");
                for (int i = 0; i < nDecimalPlaces; ++i) {
                    sb.append('0');
                }
                numberFormat = sb.toString();
            }
            decimalFormat = new DecimalFormat(numberFormat);
            
            inclusiveLimits = new Double[] { limits_[0], limits_[1] };
            if (! includeLower && (inclusiveLimits[0] != null)) {
                inclusiveLimits[0] += increment;
            }
            if (! includeUpper && (inclusiveLimits[1] != null)) {
                inclusiveLimits[1] -= increment;
            }
            
            StringBuilder sb = new StringBuilder();
            if (limits_[0] == null) {
                // No lower limit
                sb.append("( ..");
                if (limits_[1] != null) {
                    sb.append(decimalFormat.format(limits_[1]));
                    sb.append(includeUpper ? " ]" : " )");
                }
                else {
                    sb.append(')');
                }
            }
            else if (limits_[1] == null) {
                // lower limit but No upper limit
                sb.append(includeLower ? "[ " : "( ");
                sb.append(decimalFormat.format(limits_[0]));
                sb.append(".. )");
            }
            else {
                sb.append(includeLower ? "[ " : "( ");
                sb.append(decimalFormat.format(limits_[0]));
                sb.append(" .. ");
                sb.append(decimalFormat.format(limits_[1]));
                sb.append(includeUpper ? " ]" : " )");
            }
            expression = sb.toString();
        }
        
        @Override
        public String getExpression() {
            return expression;
        }

        public double getIncrement() {
            return increment;
        }
        
        public Double[] get_Limits() {
            return limits_;
        }
        
        public Double[] getInclusiveLimits() {
            return inclusiveLimits;
        }
        
        public int getNumberOfDecimalPlaces() {
            return nDecimalPlaces;
        }
        
        public boolean getIncludesLowerBound() {
            return includeLower;
        }
        
        public boolean getIncludesUpperBound() {
            return includeUpper;
        }
        
        public DecimalFormat getDecimalFormat() {
            return decimalFormat;
        }
        public String getNumberFormat() {
            return numberFormat;
        }

        @Override
        public boolean test(String t) {
            boolean result = false;
            try {
                double v = Double.parseDouble(t);
                if (! Double.isNaN(v)) {
                    result = inclusiveLimits[0] <= v && v <= inclusiveLimits[1];
                }
            }
            catch (NumberFormatException e) {
                // bad number therefore fails the test
            }
            return result;
        }

    }
    
    static public class Choice extends KnobValidationRule {
        
        private final Set<String> choices = new LinkedHashSet<>();
        private final String expression;
        
        Choice(List<String> list) throws InvalidRuleException {
            
            Set<String> check = new HashSet<>();
            for (String ch : list) {
                check.add(ch.toLowerCase());
            }
            if (check.size() != list.size()) {
                throw new InvalidRuleException("choices must be unique (case-insensitively)");
            }
            choices.addAll(list);
            if (choices.size() < 1) {
                throw new InvalidRuleException(
                        String.format("Insufficient choices (%d) for CATEGORICAL",
                                choices.size()));
            }
            
            expression = choices.stream().collect(Collectors.joining("|"));
        }
        
        @Override
        public String getExpression() {
            return expression;
        }
        
        public List<String> getChoices() {
            return new ArrayList<>(choices);
        }

        @Override
        public boolean test(String t) {
            return choices.contains(t);
        }
    }

    static public class JavaScriptRule extends KnobValidationRule {
        
        private ScriptEngine engine;
        private String code;

        public JavaScriptRule(String input) throws InvalidRuleException {
            this.code = input;
            engine = new ScriptEngineManager().getEngineByName("javascript");
        }
        
        public Either<ScriptException,Object> execute(Map<String,Object> inputs) {
            ScriptContext context = engine.getContext();
            if (! Check.isEmpty(inputs)) {
                for (String name : inputs.keySet()) {
                    context.setAttribute(name, inputs.get(name), ScriptContext.ENGINE_SCOPE);
                }
            }
            try {
                return Either.right(engine.eval(code));
            }
            catch (ScriptException e) {
                return Either.left(e);
            }
        }
        
        @Override
        public String getExpression() {
            return "javascript:" + code;
        }

        @Override
        public boolean test(String t) {
            return true;
        }
    }

}
