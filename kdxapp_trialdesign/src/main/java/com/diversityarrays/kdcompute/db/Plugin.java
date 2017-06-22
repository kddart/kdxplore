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
package com.diversityarrays.kdcompute.db;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import javax.persistence.UniqueConstraint;

import com.diversityarrays.kdcompute.db.helper.DbUtil;
import com.diversityarrays.kdcompute.db.helper.FutureWork;
import com.google.gson.Gson;

/**
 * Provides the basic information required for a KDCompute plugin.
 * <p>
 * This information may be used to automatically generate a simple user interface
 * for either a web or desktop UI to gather the values and input data files
 * required to run an analysis program.
 * <p>
 * To actually run, each <i>Knob</i> (a.k.a <i>parameter</i>) must have a value supplied
 * as must each <i>Input Data Set</i>.
 * @see {@link RunBinding} and {@link AnalysisRequest}
 * 
 * 
 * @author brianp
 *
 */
@SuppressWarnings("nls")
@Entity
@Table(
		uniqueConstraints=
		@UniqueConstraint(columnNames={"pluginName", "versionCode"})
		)
public class Plugin implements Comparable<Plugin> {

	public static final String PluginName = "pluginName";
	public static final String AlgorithmName  = "algorithmName";


	@Id
	@GeneratedValue
	@Column(name = "Plugin_Id", nullable= false)
	private Long id;

	@Column(name = AlgorithmName, nullable=false)
	private String algorithmName;


	/**
	 * The name of the plugin that provides the
	 * code. This is usually the name from the repository
	 * and matches the file system folder name
	 * in which the Algorithm is installed.
	 */
	@Column(name = PluginName, nullable = false)
	private String pluginName;

	/**
	 * A monotonically increasing number.
	 */
	@Column(nullable = false)
	private int versionCode = 1;

	/**
	 * User visible version.
	 */
	@Column(name = "versionString",nullable = true)
	private String versionString;

	@Column(name = "author",nullable = true)
	private String author;

	@Lob
	@Column(name = "description", nullable = true)
	private String description;

	@Column(name = "legacy", nullable=true)
	private boolean legacy;


	@ElementCollection(fetch=FetchType.EAGER)
	@CollectionTable(
			name="Plugin_knobs",
			joinColumns=@JoinColumn(name="Plugin_Id"))

//	@Cascade(value=org.hibernate.annotations.CascadeType.ALL)
	private List<Knob> knobs = new ArrayList<>();

//	@LazyCollection(LazyCollectionOption.FALSE)
	@ElementCollection
	@CollectionTable(
			name="Plugin_inputDataSets",
			joinColumns = @JoinColumn(name="Plugin_Id"))
//	@Cascade(value=org.hibernate.annotations.CascadeType.ALL)
	private List<DataSet> inputDataSets = new ArrayList<>();

	// TODO Future - output data sets
	//    @ElementCollection
	//    @OrderColumn(name = "output_order")
	//    protected List<DataSet> outputDataSets = new ArrayList<>();

	/**
	 * This is path identifies the template for the script that will
	 * actually run the algorithm and is relative to the root directory of
	 * the plugin in the "algorithms" folder.
	 */
	private String scriptTemplateFilename;

	/**
	 * This is path identifies the template for the HTML page that will
	 * actually run the algorithm and is relative to the root directory of
	 * the plugin in the "algorithms" folder.
	 */
	@Column(nullable = true)
	private String htmlFormTemplateFilename;

	@Column(nullable = true)
	private String htmlHelp;

	@Column(nullable = true)
	private String htmlHelpUrl;

	@Column(nullable = true)
	private String docUrl;

	@Temporal(TemporalType.TIMESTAMP)
	@Column(nullable = true)
	private Date whenLastUpdated;

	public Plugin() {
	}

	public Plugin(String n, int versionCode) {
		this();
		algorithmName = n;
		this.versionCode = versionCode;
	}

	public static Plugin createPluginFromFile(File file) throws IOException {
		if(!file.exists()) {
			throw new FileNotFoundException("File "+file.getAbsolutePath()+" does not exist!");
		}
		return new Gson().fromJson(new FileReader(file), Plugin.class);
	}

	public Long getId() {
		return id;
	}

	@Override
	public int hashCode() {
		return algorithmName.hashCode() * 17 + Integer.hashCode(versionCode);
	}

	@Override
	public boolean equals(Object o) {
		if (this == o) return true;
		if (! (o instanceof Plugin)) return false;
		Plugin other = (Plugin) o;
		return this.algorithmName.equals(other.algorithmName)
				&&
				this.versionCode == other.versionCode;
	}

	@FutureWork
	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder(getClass().getSimpleName());
		sb.append('(');
		if (isLegacy()) {
			sb.append("OLD ");
		}
		sb.append("id=").append(id);
		//        sb.append(' ').append(visibility);
		sb.append(" '").append(algorithmName).append('\'');
		DbUtil.appendListInfo(sb, ", knobs", knobs, Knob::getKnobName);
		DbUtil.appendListInfo(sb, "  inputs", inputDataSets, DataSet::getDataSetName);

		//      DbUtil.appendListInfo(sb, "  outputs", outputDataSets, DataSet::getDataSetName);

		sb.append(")");

		return sb.toString();
	}

	/**
	 * The user visible name of the Algorithm.
	 * @return
	 */
	public String getAlgorithmName() {
		return algorithmName;
	}

	public void setAlgorithmName(String n) {
		this.algorithmName = n;
	}

	/**
	 * If provided, this is the filename containing the
	 * HTML template used to display the
	 * user interface for this Algorithm.
	 * @return String or null
	 */
	public String getHtmlFormTemplateFilename() {
		return htmlFormTemplateFilename;
	}
	public void setHtmlFormTemplateFilename(String filename) {
		this.htmlFormTemplateFilename = filename;
	}


	/**
	 * This provides the (usually BASH) filename of the script template in which the various
	 * <code>Knob</code> variable place-holders will be replaced with the values
	 * of the <code>Knob</code>s.
	 * <p>
	 * Note that this filename/path is relative to the root directory that holds all of the algorithms.
	 * In the old KDCompute, this directory is named <code>algorithms</code>.
	 * @return String
	 */
	public String getScriptTemplateFilename() {
		return scriptTemplateFilename;
	}
	public void setScriptTemplateFilename(String filename) {
		scriptTemplateFilename = filename;
	}

	/**
	 * Return the list of Knobs that must be provided with values for this algorithm.
	 * @return List
	 */
	public List<Knob> getKnobs() {
		return Collections.unmodifiableList(knobs);
	}

	/**
	 * Replace the knobs with this list.
	 * @param list
	 */
	public void setKnobs(List<Knob> list) {
		this.knobs = list;
	}

	/**
	 * Return all of the input DataSets of this algorithm.
	 * @return Collection
	 */    
	public Collection<DataSet> getInputDataSets() {
		return Collections.unmodifiableList(inputDataSets);
	}

	/**
	 * Replace the inputs with the supplied list.
	 * @param list
	 */
	public void setInputDataSets(List<DataSet> list) {
		inputDataSets = list;
	}

	@Override
	public int compareTo(Plugin o) {
		int diff = getAlgorithmName().compareTo(o.getAlgorithmName());
		if (diff == 0) {
			diff = Integer.compare(getKnobs().size(), o.getKnobs().size());
			if (diff == 0) {
				diff = Integer.compare(getInputDataSets().size(), o.getInputDataSets().size());
				if (diff == 0) {

				}
			}
		}
		return 0;
	}

	public String getDocUrl() {
		return docUrl;
	}

	public void setDocUrl(String docUrl) {
		this.docUrl = docUrl;
	}

	/**
	 * Return the name of the plugin that provides this Algorithm.
	 * @return
	 */
	public String getPluginName() {
		return pluginName;
	}

	public void setPluginName(String pluginName) {
		this.pluginName = pluginName;
	}

	/**
	 * Return an internal version code number for this Algorithm.
	 * This should be a monotonically increasing number.
	 * @return int
	 */
	public int getVersionCode() {
		return versionCode;
	}

	public void setVersionCode(int versionCode) {
		this.versionCode = versionCode;
	}

	/**
	 * Return a user-visible string that identifies the version of
	 * this algorithm.
	 * @return
	 */
	public String getVersionString() {
		return versionString;
	}

	public void setVersionString(String versionString) {
		this.versionString = versionString;
	}

	/**
	 * Return the author of this algorithm.
	 * @return String
	 */
	public String getAuthor() {
		return author;
	}
	public void setAuthor(String author) {
		this.author = author;
	}

	/**
	 * Return a description of the algorithm.
	 * @return String
	 */
	public String getDescription() {
		return description;
	}

	public void setDescription(String description) {
		this.description = description;
	}

	/**
	 * Return the on-screen help for this algorithm.
	 * @see {@link #getHtmlHelpUrl()}
	 * @return String or null
	 */
	public String getHtmlHelp() {
		return htmlHelp;
	}
	public void setHtmlHelp(String html) {
		this.htmlHelp = html;
	}
	/**
	 * Return the URL of the location of help for this Algortihm.
	 * Note that if this is a file:// then the path component is
	 * taken to be relative to the root folder of the algorithm.
	 * @see {@link #getHtmlHelp()}
	 * @return String or null
	 */
	public String getHtmlHelpUrl() {
		return htmlHelpUrl;
	}
	public void setHtmlHelpUrl(String url) {
		htmlHelpUrl = url;
	}

	/**
	 * Return whether this Algorithm is a legacy style.
	 * That implies that the scriptTemplateFile uses the old
	 * style templating mechanism.
	 * @return boolean
	 */
	public boolean isLegacy() {
		return legacy;
	}
	public void setLegacy(boolean b) {
		this.legacy = b;
	}

	/**
	 * Return when this algorithm was last updated.
	 * @return Date or null if never updated
	 */
	public Date getWhenLastUpdated() {
		return whenLastUpdated;
	}

	public void setWhenLastUpdated(Date whenLastUpdated) {
		this.whenLastUpdated = whenLastUpdated;
	}


}
