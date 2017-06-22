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
package com.diversityarrays.kdxplore.trialdesign.algorithms;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import com.diversityarrays.kdcompute.db.KnobBinding;
import com.diversityarrays.kdcompute.db.RunBinding;

import net.pearcan.util.StringUtil;

@SuppressWarnings("nls")
public class RunSpec {

	private static final boolean DEBUG = Boolean.getBoolean("DEBUG_RUNSPEC");

    public final File outputFolder;
    public final File treatmentFile;
    public final RunBinding runBinding;
    public final String rScriptCommand;

    public RunSpec(RunBinding runBinding,
            File outputFolder,
            File treatmentFile,
            String rcmd)
    {
        this.runBinding = runBinding;
        this.outputFolder = outputFolder;
        this.treatmentFile = treatmentFile;

        String cmd = rcmd;
//        cmd = cmd.replace("$rns", "--rns $rns");
        cmd = cmd.replace("--outputdir ./", "--outputdir $OUTPUTDIR");
        cmd = cmd.replace("Rscript", "$RSCRIPT");
        cmd = cmd.replaceAll(" ",",");

        this.rScriptCommand = cmd;
    }

    public String constructRunCommand(
            RunBinding rb,
            String rScriptPath)
    throws IOException
    {
        File algorithmDir = outputFolder;

        Map<String, String> tokenValues = new HashMap<>();

        if (DEBUG) {
        	System.out.println(rb.getKnobBindings().stream()
        			.map(kb -> kb.getKnob().getKnobName() + "=" + kb.getKnobValue())
        			.collect(Collectors.joining("\n\t", "----- RunSpec Knob Bindings:\n\t", "-----")));
        }

        tokenValues.put("OUTPUTDIR", outputFolder.getPath());
        tokenValues.put("ALGORITHMDIR", algorithmDir.getPath() );
        tokenValues.put("RSCRIPT", rScriptPath);

        //        Optional<KnobBinding> opt_kb = rb.getKnobBindings().stream().filter(kb -> KnobDataType.FILE_UPLOAD == kb.getKnob().getKnobDataType())
        //            .findFirst();
        //        if (! opt_kb.isPresent()) {
        //            throw new IOException("No FILE_UPLOAD Knob in bindings");
        //        }
        //
        //      tokenValues.put(opt_kb.get().getKnob().getKnobName(), treatmenFile.getPath());

        for (KnobBinding kb : rb.getKnobBindings()) {
            tokenValues.put(kb.getKnob().getKnobName().toUpperCase(), kb.getKnobValue());
            // Knob References in bash script are always made upper case in KDC
        }

        Set<String> missing = null;

        Pattern pattern = Pattern.compile("\\$([A-Za-z][A-Za-z0-9]*)");
        Matcher m = pattern.matcher(rScriptCommand + " ");

        StringBuffer sb = new StringBuffer();
        while (m.find()) {
            String p = m.group(1);
            String v = tokenValues.get(p);
            if (v==null) {
                if (missing==null) {
                    missing = new TreeSet<>();
                }
                missing.add(p);
            }
            else {
                m.appendReplacement(sb, v);
            }
        }

        m.appendTail(sb);

        if (missing!=null) {
            throw new IOException("Plugin Configuration Error: Missing value(s) for\n"+StringUtil.join("\n  ", missing));
        }

        return sb.toString();
    }

}
