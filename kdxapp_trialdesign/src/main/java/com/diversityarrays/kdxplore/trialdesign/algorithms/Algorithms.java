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

import java.awt.image.BufferedImage;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.BiConsumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;

import javax.imageio.ImageIO;

import com.diversityarrays.kdcompute.db.Knob;
import com.diversityarrays.kdcompute.db.KnobBinding;
import com.diversityarrays.kdcompute.db.KnobDataType;
import com.diversityarrays.kdcompute.db.Plugin;
import com.diversityarrays.kdcompute.db.PluginCollection;
import com.diversityarrays.kdcompute.db.RunBinding;
import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.trialdesign.TrialEntry;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.Either;
import com.diversityarrays.util.ListByOne;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import net.pearcan.util.StringUtil;

/**
 * This class is responsible for loading all of the Algorithms
 * available for TrialDesign.
 * <p>
 * It all starts with the resource <code>plugin_collection.json</code>.
 * Note that the <code>resources</code> source area contains
 * this resource in the correctly named package.
 * <p>
 * Initial support is only for the <i>Agricolae</i> algorithms
 * and it is assumed that they will all use <code>Rscript</code>.
 * <p>
 * Currently, because of some issues in the normalisation of commands
 * in the individual <code>plugin.json</code> files the commands have all
 * been collected into a file named <code>runcommand.txt</code> in the
 * <i>Agricolae</i> plugin_collection directory.
 *
 * @author brianp
 *
 */
@SuppressWarnings("nls")
public class Algorithms {

    /**
     * This constrains the available Algorithms to those which have at most this many FILE_UPLOAD
     * Knobs.
     */
    static private int MAX_FILE_UPLOADS = 1;


    public enum ErrorType {
        MISSING_PLUGIN_JSON("missing plugin.json"),
        NO_DOUBLE_TREATMENTS("double treatment columns not yet supported"),
        RSCRIPT_COMMAND("Rscript problem"),
        JSON_ERROR("plugin.json error"),
        INIT_ERROR("Initialisation error"),
        ;

        public final String description;
        ErrorType(String d) {
            description = d;
        }
    }

    static private final boolean DEBUG = Boolean.getBoolean(Algorithms.class.getName() + ".DEBUG");

    static private final String AGRICOLAE_TRIAL_DESIGN =  "kdcp_standalone_agricolaeTrialDesign/";

    public static final String TAG = Algorithms.class.getSimpleName();

    static public String readContent(String heading, InputStream is) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        if (! Check.isEmpty(heading)) {
            pw.println(heading);
        }
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (null != (line = br.readLine())) {
                pw.println(line);
            }
        }
        catch (IOException e) {
            pw.println("--- Error while reading ");
            e.printStackTrace(pw);
        }
        finally {
            if (br != null) {
                try { br.close(); } catch (IOException ignore) {}
            }
            if (is != null) {
                try { is.close(); } catch (IOException ignore) {}
            }
        }

        pw.close();
        return sw.toString();
    }

    static private Optional<Exception> readContent(InputStream is, Predicate<String> lineVisitor) {
        BufferedReader br = null;
        try {
            br = new BufferedReader(new InputStreamReader(is));
            String line;
            while (null != (line = br.readLine())) {
                if (! lineVisitor.test(line)) {
                    break;
                }
            }
        }
        catch (IOException e) {
            return Optional.of(e);
        }
        finally {
            if (br != null) {
                try { br.close(); } catch (IOException ignore) {}
            }
        }
        return Optional.empty();
    }

    static public String makeResourceName(String tail) {
        String result = /*"algorithms/" + */AGRICOLAE_TRIAL_DESIGN + tail;
        if (DEBUG) {
            System.out.println(String.format("makeResourceName(%s) = %s", tail, result));
        }
        return result;
    }

    static public String makeResourceName(Plugin plugin, String tail) {
        return /*"algorithms/" + */AGRICOLAE_TRIAL_DESIGN + plugin.getPluginName() + "/" + tail;
    }

    static public InputStream getResourceStream(String tail) throws IOException {
        String resourceName = makeResourceName(tail);
        InputStream is = Algorithms.class.getResourceAsStream(resourceName);
        if (is == null) {
            throw new IOException(resourceName);
        }
        return is;
    }

    static public Either<Exception, PluginCollection> loadPluginCollection() {
        try {
            InputStream is = getResourceStream("plugin_collection.json");
            PluginCollection pluginCollection = new GsonBuilder().create().fromJson(
                    new InputStreamReader(is), PluginCollection.class);
            return Either.right(pluginCollection);
        }
        catch (JsonSyntaxException | IOException e) {
            return Either.left(e);
        }
    }


    private Map<Plugin, String> rScriptCommandByPlugin = new HashMap<>();

    private Map<Plugin, PluginInfo> pluginInfoByPlugin = new HashMap<>();

    private PluginCollection pluginCollection;

    private Exception pluginCollectionException;


    private final ListByOne<ErrorType, String> errors = new ListByOne<>();


    public Algorithms() {

        Either<Exception, PluginCollection> either = loadPluginCollection();
        if (either.isLeft()) {
            pluginCollectionException = either.left();
            errors.addKeyValue(ErrorType.INIT_ERROR,
                    pluginCollectionException.getMessage());
        }
        else {
            pluginCollection= either.right();

            if (pluginCollection.plugins==null || pluginCollection.plugins.length <= 0) {
                errors.addKeyValue(ErrorType.INIT_ERROR, "No Plugins");
            }
            else {
                Either<String, Map<Plugin, String>> either2 = loadRscriptByPlugin();
                if (either.isLeft()) {
                    errors.addKeyValue(ErrorType.INIT_ERROR, either2.left());
                }
                else {
                    this.rScriptCommandByPlugin = either2.right();
                    initAlgorithmDirnameByPlugin();
                }
            }
        }
    }

    public ListByOne<ErrorType, String> getErrors() {
        return errors;
    }

    public Either<Exception, PluginCollection> getPluginCollection() {
        if (pluginCollectionException != null) {
            return Either.left(pluginCollectionException);
        }
        return Either.right(pluginCollection);
    }

    public PluginHelp getPluginHelp(Plugin p) {
        PluginInfo info = pluginInfoByPlugin.get(p);
        return info==null ? null : info.pluginHelp;
    }

    public int getAlgorithmCount() {
        return pluginInfoByPlugin.size();
    }

    public Collection<Plugin> getSortedPlugins() {
        if (pluginCollectionException != null) {
            throw new IllegalStateException("No PluginCollection", pluginCollectionException);
        }
        List<Plugin> result = new ArrayList<>(pluginInfoByPlugin.keySet());
        Collections.sort(result, new Comparator<Plugin>() {
            @Override
            public int compare(Plugin o1, Plugin o2) {
                return o1.getAlgorithmName().compareTo(o2.getAlgorithmName());
            }
        });
        return result;
    }

    private String collectRscriptCommand(RunBinding rb) throws IOException {
        String rScriptCommand = rScriptCommandByPlugin.get(rb.getPlugin());
        int pos = rScriptCommand.indexOf(".R ");
        if (pos <= 0) {
            throw new IOException("Invalid format for RScript command: " + rScriptCommand);
        }
        String[] pathElements = rScriptCommand.substring(0, pos+2).split("/", 0);
        String fileName = pathElements[pathElements.length-1];
        return fileName;
    }


    static public File makeTempOutputFolder() throws IOException {
        File tmp = File.createTempFile("kdxplore", ".tmp");
        tmp.delete();
        if (! tmp.mkdirs()) {
            throw new IOException("Unable to create working directory: " + tmp.getPath());
        }
        return tmp;
    }


    public String getRscriptCommand(Plugin plugin) {
        return rScriptCommandByPlugin.get(plugin);
    }

    public Either<String, RunSpec> createRunSpec(
            RunBinding rb,
            List<TrialEntry> entries)
            throws IOException
    {
        PluginInfo info = pluginInfoByPlugin.get(rb.getPlugin());
        if (info == null) {
            return Either.left("Missing data for plugin");
        }

        InputStream stream = null;
        try {
            String fileName = collectRscriptCommand(rb);

            File outputFolder = makeTempOutputFolder();

            File treatmentFile = new File(outputFolder, CsvTreatmentWriter.TREATMENT_CSV);
            CsvTreatmentWriter tw = new UnQuotedCsvTreatmentWriter();
            tw.writeEntries(rb, entries, treatmentFile);

            String resourceName = makeResourceName(info.dirname + "/" + fileName);

            stream = Algorithms.class.getResourceAsStream(resourceName);

            File scriptFile = new File(outputFolder, fileName);
            Files.copy(stream, scriptFile.toPath(), StandardCopyOption.REPLACE_EXISTING);

            for (Knob knob : rb.getUnboundKnobs()) {
                if (KnobDataType.FILE_UPLOAD == knob.getKnobDataType()) {
                    KnobBinding binding = new KnobBinding(knob);
                    binding.setKnobValue(treatmentFile.getAbsolutePath());
                    rb.addKnobBinding(binding);
                }
            }

            String rScriptCommand = rScriptCommandByPlugin.get(rb.getPlugin());

            //            if (rParams !=null && ! Check.isEmpty(rParams)) {
            //                String params = makeParamsString(rb, rParams,algOutputFolder,treatmentFilePath, scriptFile.getParentFile().getAbsolutePath());
            //                System.out.println(params);
            //
            //            }
            if (DEBUG) {
                System.out.println("Fully Bound=" + rb.isFullyBound());
                String line = rb.getKnobBindings().stream()
                        .map(kb -> kb.getKnob().getKnobName() + "=" +  kb.getKnobValue())
                        .collect(Collectors.joining("\n", "Parameter Values:\n", ""));
                System.out.println(line);
            }

            if (Check.isEmpty(rScriptCommand)) {
                return Either.left("Missing runScript command");
            }

            RunSpec result = new RunSpec(rb, outputFolder, treatmentFile, rScriptCommand);

            return Either.right(result);
        } finally {
            if (stream != null) {
                try { stream.close(); }
                catch (IOException ignore) {}
            }
        }

    }


    private void initAlgorithmDirnameByPlugin() {

        errors.clear();

        Gson gson = new GsonBuilder().create();


        BiConsumer<ErrorType, String> onError = new BiConsumer<ErrorType, String>() {
            @Override
            public void accept(ErrorType t, String u) {
                errors.addKeyValue(t, u);
            }
        };

        BiConsumer<String, PluginHelp> onPluginFound = new BiConsumer<String, PluginHelp>() {
            @Override
            public void accept(String pluginName, PluginHelp help) {
                pluginInfoByPlugin.put(help.plugin, new PluginInfo(pluginName, help));
            }
        };

        for (String pluginName : pluginCollection.plugins) {

            String resourceName = makeResourceName(pluginName + "/plugin.json");

            doit(gson, pluginName, resourceName, onPluginFound, onError);
        }
    }

    private void doit(Gson gson, String pluginName, String resourceName,
            BiConsumer<String, PluginHelp> onPluginFound,
            BiConsumer<ErrorType, String> onError)
    {
        InputStream is = null;
        try {
            is = getClass().getResourceAsStream(resourceName);
            if (is == null) {
                onError.accept(ErrorType.MISSING_PLUGIN_JSON, pluginName);
            }
            else {
                try {
                    Plugin plugin = gson.fromJson(new InputStreamReader(is), Plugin.class);

                    List<Knob> fileUploadKnobs = plugin.getKnobs().stream()
                            .filter(k-> KnobDataType.FILE_UPLOAD == k.getKnobDataType())
                            .collect(Collectors.toList());

//                        String s = fileUploadKnobs.stream().map(k -> k.getKnobName())
//                                .collect(Collectors.joining(", ",
//                                        plugin.getAlgorithmName() + ": FILE_UPLOADS=",
//                                        ""));
//                        System.out.println(s);

                    List<CsvTreatmentWriter.TreatmentHeading> headings =
                            CsvTreatmentWriter.getTreatmentHeadings(pluginName);
                    if (headings != null
                            &&
                        headings.contains(CsvTreatmentWriter.TreatmentHeading.TREATMENT_2))
                    {
                        onError.accept(ErrorType.NO_DOUBLE_TREATMENTS, pluginName);
                        return;
                    }

                    String scriptTemplateResourceName = makeResourceName(plugin,
                            plugin.getScriptTemplateFilename());

                    Either<Exception, String> either2 = readRscriptCommand(scriptTemplateResourceName);
                    if (either2.isLeft()) {
                        onError.accept(ErrorType.RSCRIPT_COMMAND,
                                pluginName + ": " + either2.left().getMessage());
                        return;
                    }

                    if (fileUploadKnobs.size() <= MAX_FILE_UPLOADS
                            &&
                            rScriptCommandByPlugin.containsKey(plugin))
                    {
                        String c0 = rScriptCommandByPlugin.get(plugin);
                        String c1 = either2.right();
                        if (! c0.equals(c1)) {
                            System.err.println("Commands differ for " + pluginName);
                            System.err.println("  rctxt:" + c0);
                            System.err.println("  tmplt:" + c1);
                        }

                        PluginHelp pluginHelp = new PluginHelp(plugin);
                        onPluginFound.accept(pluginName, pluginHelp);
                    }
                }
                catch (JsonSyntaxException e) {
                    onError.accept(ErrorType.JSON_ERROR, pluginName + ": " + e.getMessage());
                }
            }
        }
        finally {
            if (is != null) {
                try { is.close(); } catch (IOException ignore) { }
            }
        }
    }

    static public class PluginInfo {
        public final String dirname;
        public final PluginHelp pluginHelp;

        PluginInfo(String dir, PluginHelp h) {
            this.dirname = dir;
            this.pluginHelp = h;
        }
    }

    static public class PluginHelp {

        static private final String HELP_PREFIX = "/help/";
        public final BufferedImage logo;
        public final String englishHtml;
        public final Plugin plugin;

        PluginHelp(Plugin plugin) {
            this.plugin = plugin;
            englishHtml = loadEnglishHtml();

            logo = loadLogoImage();
        }

        private BufferedImage loadLogoImage() {
            String resourceName = makeResourceName(
                    plugin.getPluginName() + HELP_PREFIX + "resources/logo.png");
            InputStream is = PluginHelp.class.getResourceAsStream(resourceName);
            if (is == null) {
                Shared.Log.w(TAG, "Missing logo resource: " + resourceName);
                return null;
            }
            try {
                BufferedImage img = ImageIO.read(is);
                System.out.println(String.format("Plugin: %s help image is %dx%d", plugin.getPluginName(), img.getWidth(),img.getHeight()));
                return img;
            }
            catch (IOException e) {
                Shared.Log.w(TAG, "Error reading resource: " + resourceName, e);
                return null;
            }
        }

        private String loadEnglishHtml() {
            String resourceName = makeResourceName(
                    plugin.getPluginName() + HELP_PREFIX + "english.html");
            InputStream is = PluginHelp.class.getResourceAsStream(resourceName);
            if (is == null) {
                Shared.Log.w(TAG, "Missing html resource: " + resourceName);
                return "";
            }
            StringBuilder sb = new StringBuilder("<HTML>");
            String content = readContent(null, is);
            if (! Check.isEmpty(content)) {
                sb.append(content);
            }
            else {
                String desc = plugin.getDescription();
                if (! Check.isEmpty(desc)) {
                    sb.append(StringUtil.htmlEscape(desc));
                }
                else {
                    sb.append("Sorry - no description available");
                }
            }
            return sb.toString();
        }
    }

    private Either<Exception, String> readRscriptCommand(String scriptTemplateResourceName) {
        InputStream is = getClass().getResourceAsStream(scriptTemplateResourceName);
        if (is == null) {
            return Either.left(new Exception("Missing resource: " + scriptTemplateResourceName));
        }

        String[] found = new String[1];
        Predicate<String> visitor = new Predicate<String>() {
            @Override
            public boolean test(String line) {
                if (line.trim().startsWith("Rscript ")) {
                    found[0] = line.trim();
                    return false;
                }
                return true;
            }
        };
        Optional<Exception> optional = readContent(is, visitor);

        if (optional.isPresent()) {
            return Either.left(optional.get());
        }

        if (found[0] != null) {
            return Either.right(found[0]);
        }

        return Either.left(
                new Exception("Rscript command not found in " + scriptTemplateResourceName));
    }


    private Either<String, Map<Plugin, String>> loadRscriptByPlugin() {

        Map<Plugin, String> result = new HashMap<>();
        BufferedReader reader = null;
        String line = "";

        Gson gson = new GsonBuilder().create();

        try {
            InputStream resourceAsStream = Algorithms.getResourceStream("runcommand.txt");

            reader = new BufferedReader(new InputStreamReader(resourceAsStream));

            int lineNumber = 0;
            while (null != (line = reader.readLine())) {
                ++lineNumber;

                line = line.trim();
                if (line.startsWith("#") || line.isEmpty()) {
                    continue;
                }

                String[] parts = line.trim().split(":", 2);
                if (parts.length != 2) {
                    return Either.left("Invalid construct in runcommand.txt Line#" + lineNumber);
                }

                InputStream is = Algorithms.getResourceStream(parts[0] + "/plugin.json");
                Plugin plugin = gson.fromJson(new InputStreamReader(is), Plugin.class);

                result.put(plugin, parts[1].trim());
            }
        } catch (IOException e) {
            return Either.left(e.getMessage());
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
        return Either.right(result);
    }

}
