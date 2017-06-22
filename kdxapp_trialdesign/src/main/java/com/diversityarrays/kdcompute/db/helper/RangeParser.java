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

import java.text.NumberFormat;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.diversityarrays.util.Check;


public class RangeParser {

    public static final String DEFAULT_BOOLEX_VARIABLE_NAME = "x"; //$NON-NLS-1$

    static public double parseDouble(String input) throws java.text.ParseException {
        return parseDouble(input, null);
    }


    static public double parseDouble(String input, NumberFormat usNumberFormat) throws java.text.ParseException {
        if (usNumberFormat == null) {
            usNumberFormat = NumberFormat.getInstance(Locale.US);
        }
        try {
            return usNumberFormat.parse(input).doubleValue();
        }
        catch (java.text.ParseException e) {
            if (input.startsWith("+")) { //$NON-NLS-1$
                return usNumberFormat.parse(input.substring(1)).doubleValue();
            }
            throw e;
        }
    }
    
    static RangeImplParser[] PARSERS = {
            new RangeImplParser_Groovy(),      //  min .. max          min <.. max       min ..< max      min <..< max
            new RangeImplParser_Maths(),       //    [min..max]         [min..max)        (min..max]       (min..max) 
            new RangeImplParser_VarInMiddle(), // "min <= x <= max"  "min <= x < max"  "min < x <= max"  "min < x < max" 
            
            new RangeImplParser_OneComparison(), //  "0<x" , "x>0" ,  "x <= 1234"
            
            new RangeImplParser_TwoComparisons() //  "0<=x && x<=max"   etc
    };
    
    static public RangeParseResult parse(String inExpr, ValidationRuleType vrt) throws InvalidRuleException {
        String expr = inExpr;
        for (RangeImplParser rip : PARSERS) {
            RangeParseResult rpr = rip.parse(expr, vrt);
            if (rpr != null) {
                return rpr;
            }
        }
        return null;
    }

    static public class RangeParseResult {
        
        public final Double lowerLimit_;
        public final Double upperLimit_;

        public final String s_lowerLimit;
        public final String s_upperLimit;

        public final ValidationRuleType validationRuleType;
        
        RangeParseResult(ValidationRuleType v, 
                String s_lo, Double lo, 
                String s_hi, Double hi) 
        {
            s_lowerLimit = s_lo;
            lowerLimit_ = lo;
            
            s_upperLimit = s_hi;
            upperLimit_ = hi;
            validationRuleType = v;
        }
        
        @Override
        public String toString() {
            StringBuilder sb = new StringBuilder("RangeParseResult(");
            sb.append(validationRuleType);
            sb.append(": ").append(s_lowerLimit).append(" .. ").append(s_upperLimit);
            sb.append(')');
            return sb.toString();
        }

        public int computeDecimalPlaces() {
            
            if (s_lowerLimit != null) {
                if (DIGITS_ONLY.matcher(s_lowerLimit).matches()) {
                    if (s_upperLimit == null) {
                        // must be an open-ended interval
                        return 0;
                    }
                }
            
                if (DIGITS_ONLY.matcher(s_upperLimit).matches()) {
                    // Both are integers
                    return 0;
                }
            }
            else {
                // lower_limit NULL
                if (s_upperLimit == null) {
                    throw new IllegalStateException("Both s_lowerLimit and s_upperLimit are null !");
                }
                if (DIGITS_ONLY.matcher(s_upperLimit).matches()) {
                    // Only upper and it is an integer
                    return 0;
                }
            }


            String sBefore = null;
            int nBefore = 0;

            String sAfter = null;
            int nAfter = 0;
            
            Matcher m = AFTER_DECIMAL_POINT.matcher(s_lowerLimit);
            if (m.matches()) {
                sBefore = m.group(1);
                nBefore = sBefore.length();
            }
            
            m = AFTER_DECIMAL_POINT.matcher(s_upperLimit);
            if (m.matches()) {
                sAfter = m.group(1);
                nAfter = sAfter.length();
            }

            int result = Math.max(nBefore, nAfter);
            
            return result;
        }
        
        public int getValueCount(double increment) {
            
            if (lowerLimit_ == null || upperLimit_ == null) {
                return Integer.MAX_VALUE;
            }

            int nDecimalPlaces = computeDecimalPlaces();

            int result;
            
            double rangeStart = lowerLimit_;
            double rangeEnd = upperLimit_;
            
            boolean isRangeStartIncluded = validationRuleType.includesLowerLimit();
            boolean isRangeEndIncluded = validationRuleType.includesUpperLimit();
            
            if (nDecimalPlaces > 0) {

                double count = (rangeEnd - rangeStart) / increment;

                if (! isRangeStartIncluded) {
                    --count;
                }
                if (! isRangeEndIncluded) {
                    --count;
                }

                if (Double.isInfinite(count) || Double.isNaN(count) || (count > (double) Integer.MAX_VALUE)) {
                    result = Integer.MAX_VALUE;
                }
                else {
                    result = (int) count;
                }
            }
            else {
                long first = ((int) rangeStart) + (isRangeStartIncluded ? 0 : 1);
                long last  = ((int) rangeEnd)   - (isRangeEndIncluded   ? 0 : 1);

                long count = (last - first) + 1;
                result = count < Integer.MAX_VALUE ? (int) count : Integer.MAX_VALUE;
            }
            return result;
        }

        static private final Pattern AFTER_DECIMAL_POINT = Pattern.compile("^.*\\.(\\d+)$"); //$NON-NLS-1$

        static private final Pattern DIGITS_ONLY = Pattern.compile("^[-+]?\\d+$"); //$NON-NLS-1$

    }
    
    static public abstract class RangeImplParser {
        
        static public class SplitResult {
            Op op;
            String s_val;
            double val;
            
            SplitResult(Op op, String s_val, double val) {
                this.op = op;
                this.s_val = s_val;
                this.val = val;
            }
            
            @Override
            public String toString() {
                return "SplitResult(" + op + "   s_val=" + s_val + ")";
            }

            public SplitResult swapOperator() {
                return new SplitResult(op.opposite(), s_val, val);
            }
        }
        
        public enum VarOn {
            LEFT,
            RIGHT
        }
        
        public enum Op {
            LE("<="), //$NON-NLS-1$  MUS SEARCH FOR <= before <
            LT("<"), //$NON-NLS-1$
            GE(">="), //$NON-NLS-1$  MUST SEARCH FOR >= before >
            GT(">"), //$NON-NLS-1$
            ;
            public final String text;
            Op(String s) {
                text = s;
            }
            
            public Op opposite() {
                switch (this) {
                case GE:
                    return LE;
                case GT:
                    return LT;
                case LE:
                    return GE;
                case LT:
                    return GT;
                default:
                    throw new RuntimeException(name() + ".opposite");
                }
            }
            
            public boolean isGTorGE() {
                return this==GT || this==GE;
            }
        }
        
        public abstract RangeParseResult parse(String expr, ValidationRuleType vrt) throws InvalidRuleException;
        
        /**
         * Returns null or OP, value
         * @param in input
         * @param varOn which side of the operator we expect to see the variable
         * @return the operator and the string and double forms of the value
         */
        static public SplitResult splitOnOperator(String in, VarOn varOn) {
            SplitResult result = null;
                
            Op found = null;
            int opPos = 0;
            for (Op op : Op.values()) {
                opPos = in.indexOf(op.text);
                if (opPos > 0) {
                    found = op;
                    break;
                }
            }
            
            if (found != null) {
                String before = in.substring(0, opPos).trim();
                String after  = in.substring(opPos + found.text.length()).trim();
                
                if (VarOn.LEFT == varOn && DEFAULT_BOOLEX_VARIABLE_NAME.equalsIgnoreCase(before)) {
                    // Expecting  "x OP value"
                    try {
                        double val = parseDouble(after);
                        result = new SplitResult(found, after, val);
                    }
                    catch (java.text.ParseException ignore) {}
                }
                else if (DEFAULT_BOOLEX_VARIABLE_NAME.equalsIgnoreCase(after)) {
                    // Expecting  "value OP x"
                    try {
                        double val = parseDouble(before);
                        result = new SplitResult(found, before, val);
                    }
                    catch (NumberFormatException ignore) {}
                    catch (java.text.ParseException ignore) {}
                }
            }
            
            return result;
        }
        
        protected RangeParseResult lastChecks(SplitResult before, SplitResult after, String expr, ValidationRuleType vrt) 
        throws InvalidRuleException {
            double lowerLimit = before.val;
            double upperLimit = after.val;

            // We don't handle this variant (I'm too lazy to work through
            // all of the variations but it would be something like:
            
            // SLOTS:    min   OP   x   OP  max
            //   fill in the slots
            // and if any is empty then we return null
            if (before.op.isGTorGE() || after.op.isGTorGE()) {
                return null;
            }
            
            ValidationRuleType validationRuleType;
            if (Op.LE == before.op) {
                if (Op.LE == after.op) {
                    validationRuleType = ValidationRuleType.RANGE;
                }
                else {
                    validationRuleType = ValidationRuleType.RERANGE;
                }
            }
            else {
                if (Op.LE == after.op) {
                    validationRuleType = ValidationRuleType.LERANGE;
                }
                else {
                    validationRuleType = ValidationRuleType.BERANGE;
                }
            }

            if (ValidationRuleType.RANGE == validationRuleType) {
                if (! (lowerLimit <= upperLimit)) {
                    return null;
                }
            }
            else {
                if (! (lowerLimit < upperLimit)) {
                    return null;
                }
            }
            
            if (vrt != ValidationRuleType.RANGE) {
                if (validationRuleType != vrt) {
                    throw new InvalidRuleException(vrt + " incompatible with '" + expr + "'");
                }
            }
            
            return new RangeParseResult(validationRuleType, 
                    before.s_val, lowerLimit, 
                    after.s_val,  upperLimit);
        }

    }
    
    /**
     * Parses standard mathematical range expressions; using open/closed brackets
     * to indicate the range ends.
     * @author brianp
     *
     */
    static class RangeImplParser_Maths extends RangeImplParser {
        
        enum ParenOption {
            CLOSED("[", "]", ValidationRuleType.RANGE),
            OPEN_LEFT("(", "]", ValidationRuleType.LERANGE),
            OPEN_RIGHT("[", ")", ValidationRuleType.RERANGE),
            OPEN("(", ")", ValidationRuleType.BERANGE),
            ;

            public final String startsWith;
            public final String endsWith;
            public final ValidationRuleType vrt;
            ParenOption(String s, String e, ValidationRuleType vrt) {
                startsWith = s;
                endsWith = e;
                this.vrt = vrt;
            }
            
            public String getCenterIfMatch(String expr) {
                if (expr.startsWith(startsWith) && expr.endsWith(endsWith)) {
                    return expr.substring(1, expr.length()-1);
                }
                return null;
            }
        }

        @Override
        public RangeParseResult parse(String expr, ValidationRuleType vrt)
        throws InvalidRuleException {
            ParenOption found = null;
            String middle = null;
            for (ParenOption opt : ParenOption.values()) {
                middle = opt.getCenterIfMatch(expr);
                if (middle != null) {
                    found = opt;
                    break;
                }
            }
            
            if (found == null) {
                return null;
            }
            
            try {
                NumberFormat usNumberFormat = NumberFormat.getInstance(Locale.US);
                
                int dotsPos = middle.indexOf("..");

                Double lowerLimit = null;
                String s_lo = "";
                if (dotsPos > 0) {
                    // has lower limit
                    s_lo = middle.substring(0, dotsPos).trim();
                    lowerLimit = parseDouble(s_lo, usNumberFormat);
                }

                Double upperLimit = null;
                String s_hi = middle.substring(dotsPos + 2).trim();
                if  (! Check.isEmpty(s_hi)) {
                    upperLimit = parseDouble(s_hi, usNumberFormat);
                }
                
//                if (vrt != ValidationRuleType.RANGE) {
//                    if (found.vrt != ValidationRuleType.RANGE) {
//                        if (vrt != found.vrt) {
//                            throw new InvalidRuleException(vrt + " incompatible with '" + expr + "'"); //$NON-NLS-1$ //$NON-NLS-2$
//                        }
//                    }
//                }

                return new RangeParseResult(found.vrt, s_lo, lowerLimit, s_hi, upperLimit);
            }
            catch (java.text.ParseException ignore) { }

            return null;
        }
        
    }

    /**
     * Parses the standard min..max but also supports the Groovy
     * limit exclusion syntax. This will allow simpler data input
     * and Range recognition in future.
     * @author brianp
     *
     */
    static class RangeImplParser_Groovy extends RangeImplParser {
        
        enum DotsOption {
            
            // Must search from longest to shortest
            BOTH_OUT("<..<", ValidationRuleType.BERANGE), //$NON-NLS-1$
            LEFT_OUT("<..", ValidationRuleType.LERANGE),  //$NON-NLS-1$
            RIGHT_OUT("..<", ValidationRuleType.RERANGE), //$NON-NLS-1$
            NORMAL("..", ValidationRuleType.RANGE),       //$NON-NLS-1$
            ;
            
            public final String lookFor;
            public final ValidationRuleType vrt;
            DotsOption(String s, ValidationRuleType v) {
                lookFor = s;
                this.vrt = v;
            }
        }
        
        @Override
        public RangeParseResult parse(String expr, ValidationRuleType vrt) throws InvalidRuleException {
            DotsOption found = null;
            int dotsPos = 0;
            for (DotsOption dopt : DotsOption.values()) {
                dotsPos = expr.indexOf(dopt.lookFor);
                if (dotsPos > 0) {
                    found = dopt;
                    break;
                }
            }
            
            if (found == null) {
                return null;
            }
            
            try {
                NumberFormat usNumberFormat = NumberFormat.getInstance(Locale.US);

                String s_lo = expr.substring(0, dotsPos).trim();
                double lowerLimit = parseDouble(s_lo, usNumberFormat);

                String s_hi = expr.substring(dotsPos + found.lookFor.length()).trim();
                double upperLimit = parseDouble(s_hi, usNumberFormat);
                
                if (vrt != ValidationRuleType.RANGE) {
                    if (found.vrt != ValidationRuleType.RANGE) {
                        if (vrt != found.vrt) {
                            throw new InvalidRuleException(vrt + " incompatible with '" + expr + "'"); //$NON-NLS-1$ //$NON-NLS-2$
                        }
                    }
                }

                return new RangeParseResult(found.vrt, s_lo, lowerLimit, s_hi, upperLimit);
            }
            catch (java.text.ParseException ignore) { }

            return null;
        }
    }

    /**
     * Handles syntax with the variable in the middle of the inequalities.
     * <pre>min <= x <= max</pre>
     * @author brianp
     *
     */
    static class RangeImplParser_VarInMiddle extends RangeImplParser {
        @Override
        public RangeParseResult parse(String expr, ValidationRuleType vrt) throws InvalidRuleException {
            int xpos = expr.toLowerCase().indexOf(DEFAULT_BOOLEX_VARIABLE_NAME);
            if (xpos <= 0) {
                return null;
            }
            
            int vlen = DEFAULT_BOOLEX_VARIABLE_NAME.length();
            String valueOpX = expr.substring(0, xpos+vlen).trim();
            String xOpValue = expr.substring(xpos).trim();

            SplitResult before = RangeImplParser.splitOnOperator(valueOpX, VarOn.RIGHT);
            if (before == null) {
                return null;
            }
            
            SplitResult after = RangeImplParser.splitOnOperator(xOpValue, VarOn.LEFT);
            if (after == null) {
                return null;
            }

            return lastChecks(before, after, expr, vrt);            
        }
    }
    
    static class RangeImplParser_OneComparison extends RangeImplParser {

        @Override
        public RangeParseResult parse(String inExpr, ValidationRuleType vrt) throws InvalidRuleException {
            
            String expr = inExpr.trim();
            
            Double lowerLimit = null;
            Double upperLimit = null;
            
            String lower_sval = null;
            String upper_sval = null;
            
            ValidationRuleType validationRuleType = null;
            SplitResult sr = RangeImplParser.splitOnOperator(expr, VarOn.LEFT);
            if (sr != null) {
                // we must have "x OP value"
                switch (sr.op) {
                case GE: // x >= min
                    validationRuleType = ValidationRuleType.RERANGE;
                    lower_sval = sr.s_val;
                    lowerLimit = sr.val;
                    break;
                case GT: // x > min
                    validationRuleType = ValidationRuleType.BERANGE;
                    lower_sval = sr.s_val;
                    lowerLimit = sr.val;
                    break;
                case LE: // x <= max
                    validationRuleType = ValidationRuleType.LERANGE;
                    upper_sval = sr.s_val;
                    upperLimit = sr.val;
                    break;
                case LT: // x < max
                    validationRuleType = ValidationRuleType.BERANGE;
                    upper_sval = sr.s_val;
                    upperLimit = sr.val;
                    break;
                default:
                    break;
                }
            }
            else {
                sr = RangeImplParser.splitOnOperator(expr, VarOn.RIGHT);
                if (sr != null) {
                    // we must have "value OP x"
                    switch (sr.op) {
                    case GE: // max >= x ; i.e. x <= max
                        validationRuleType = ValidationRuleType.LERANGE;
                        upper_sval = sr.s_val;
                        upperLimit = sr.val;
                        break;
                    case GT: // max > x  ; i.e. x < max
                        validationRuleType = ValidationRuleType.BERANGE;
                        upper_sval = sr.s_val;
                        upperLimit = sr.val;
                        break;
                    case LE: // min <= x ; i.e. x >= min
                        validationRuleType = ValidationRuleType.RERANGE;
                        lower_sval = sr.s_val;
                        lowerLimit = sr.val;
                        break;
                    case LT: // min < x  ; i.e. x > min
                        validationRuleType = ValidationRuleType.BERANGE;
                        lower_sval = sr.s_val;
                        lowerLimit = sr.val;
                        break;
                    default:
                        break;
                    }
                }
            }
            
            if (validationRuleType == null) {
                return null;
            }
            
            
            return new RangeParseResult(validationRuleType, 
                    lower_sval, lowerLimit, 
                    upper_sval, upperLimit);
        }
        
    }
    
    /**
     * Handles syntax with AND of two inequalities with the variables in the center.
     * <pre>min OP x AND x OP max</pre>
     * @author brianp
     *
     */
    static class RangeImplParser_TwoComparisons extends RangeImplParser {
        
        @Override
        public RangeParseResult parse(String expr, ValidationRuleType vrt) throws InvalidRuleException {
            
            String valueOpX;
            String xOpValue;
            int andPos = expr.toLowerCase().indexOf("and");
            if (andPos > 0) {
                valueOpX = expr.substring(0, andPos).trim();
                xOpValue = expr.substring(andPos+3).trim();
            }
            else {
                andPos = expr.indexOf("&&");
                if (andPos <= 0) {
                    return null;
                }
                valueOpX = expr.substring(0, andPos).trim();
                xOpValue = expr.substring(andPos+2).trim();
            }
            
            SplitResult after;
            SplitResult before = RangeImplParser.splitOnOperator(valueOpX, VarOn.RIGHT);
            if (before == null) {
                before = RangeImplParser.splitOnOperator(valueOpX, VarOn.LEFT);
                if (before == null) {
                    // Bugger ! Give up now.
                    return null;
                }
                // need to swap the operator around
                before = before.swapOperator();
                
                after = RangeImplParser.splitOnOperator(xOpValue, VarOn.LEFT);
                if  (after == null) {
                    after = RangeImplParser.splitOnOperator(xOpValue, VarOn.RIGHT);
                    if (after == null) {
                        return null;
                    }
                }
            }
            else {
                // We have "before"
                after = RangeImplParser.splitOnOperator(xOpValue, VarOn.LEFT);
                if (after == null) {
                    after = RangeImplParser.splitOnOperator(xOpValue, VarOn.RIGHT);
                    if (after == null) {
                        return null;
                    }
                    after = after.swapOperator();
                }
            }
            return lastChecks(before, after, expr, vrt);
        }
    }
 
}
