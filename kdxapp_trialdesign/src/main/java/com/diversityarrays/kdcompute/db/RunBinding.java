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

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.persistence.CollectionTable;
import javax.persistence.Column;
import javax.persistence.ElementCollection;
import javax.persistence.Entity;
import javax.persistence.FetchType;
import javax.persistence.GeneratedValue;
import javax.persistence.Id;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

import com.diversityarrays.util.Check;

/**
 * Provides the values needed by an AnalysisJob to be run.
 * 
 * @author brianp
 *
 */
@Entity
@Table
public class RunBinding {

	@Id
	@GeneratedValue
	@Column(name = "Runbinding_Id", nullable = false)
	private Long id;

	@ManyToOne(fetch = FetchType.EAGER)
	private final Plugin plugin;

//	@LazyCollection(LazyCollectionOption.FALSE)
	@ElementCollection   
	@CollectionTable(
            name="RunBinding_knobBindings",
            joinColumns=@JoinColumn(name="Runbinding_Id"))
	private List<KnobBinding> knobBindings = new ArrayList<KnobBinding>();

//	@LazyCollection(LazyCollectionOption.FALSE)
	@ElementCollection
	@CollectionTable(
            name="RunBinding_inputDataSetBindings",
            joinColumns=@JoinColumn(name="Runbinding_Id"))
	private List<DataSetBinding> inputDataSetBindings = new ArrayList<DataSetBinding>();

	private String outputFolderPath;

	public String getPostCompletionUrl() {
		return postCompletionUrl;
	}

	public void setPostCompletionUrl(String postCompletionUrl) {
		this.postCompletionUrl = postCompletionUrl;
	}
	private String postCompletionUrl;
	
	public RunBinding() {
		this(null);
	}

	public RunBinding(Plugin a) {
		this.plugin = a;
	}

	@Override
	public String toString() {
		StringBuilder sb = new StringBuilder("RunBinding(");
		sb.append("alg='").append(plugin.getAlgorithmName()).append('\'');
		if (knobBindings.isEmpty()) {
			sb.append(" NO knob bindings");
		}
		else {
			sb.append(knobBindings.stream()
					.map(kb -> kb.getKnob().getKnobName() + "=" + kb.getKnobValue())
					.collect(Collectors.joining(" ,", " with [", "]")));
		}
		sb.append("\n  and ");
		if (inputDataSetBindings.isEmpty()) {
			sb.append("NO dataset bindings");
		}
		else {
			sb.append(inputDataSetBindings.stream()
					.map(dsb -> dsb.getDataSet().getDataSetName() + " as " + dsb.getDataSetUrl())
					.collect(Collectors.joining("\n  and ")));
		}
		sb.append(')');
		return sb.toString();
	}

	public Long getId() {
		return id;
	}

	public Plugin getPlugin() {
		return plugin;
	}

	public List<KnobBinding> getKnobBindings() {
		return Collections.unmodifiableList(knobBindings);
	}

	public void addKnobBinding(KnobBinding b) {
		if (b.getKnob() == null) {
			throw new IllegalArgumentException("KnobBinding has no Knob");
		}
		if (Check.isEmpty(b.getKnobValue())) {
			throw new IllegalArgumentException("No knobValue for " + b.getKnob().getVisibleName());
		}
		knobBindings.add(b);
	}

	public List<DataSetBinding> getInputDataSetBindings() {
		return Collections.unmodifiableList(inputDataSetBindings);
	}

	public void addInputDataSetBinding(DataSetBinding input) {
		if (input.getDataSet() == null) {
			throw new IllegalArgumentException(DataSetBinding.class.getSimpleName() + " has no DataSet");
		}
		if (Check.isEmpty(input.getDataSetUrl())) {
			throw new IllegalArgumentException("Binding for '" + input.getDataSet().getDataSetName() + "' has no url");
		}

		int found = inputDataSetBindings.indexOf(input);
		DataSet lookingFor = input.getDataSet();
		for (int index = inputDataSetBindings.size(); --index >= 0; ) {
			// Note: value semantics in DataSet
			if (inputDataSetBindings.get(index).getDataSet().equals(lookingFor)) {
				found = index;
				break;
			}
		}

		if (found >= 0) {
			inputDataSetBindings.set(found, input);
		}
		else {
			inputDataSetBindings.add(input);
		}
	}

	public String getOutputFolderPath() {
		return outputFolderPath;
	}

	public void setOutputFolderPath(String p) {
		this.outputFolderPath = p;
	}

	public Set<Knob> getUnboundKnobs() {

		// Start with all ...
		Set<Knob> unboundKnobs = new HashSet<>(plugin.getKnobs());

		// Then remove all of the bound ones...
		if (! unboundKnobs.isEmpty()) {
			Set<Knob> bound = knobBindings.stream()
					.map(KnobBinding::getKnob)
					.collect(Collectors.toSet());

			unboundKnobs.removeAll(bound);
		}
		// ... and the answer is !
		return unboundKnobs;
	}

	public boolean isFullyBound() {
		return getUnboundKnobs().isEmpty() && getUnboundDataSets().isEmpty();
	}

	public Set<DataSet> getUnboundDataSets() {

		// Start with all ...
		Set<DataSet> unboundDataSets = new HashSet<>(plugin.getInputDataSets());

		// Then remove all of the bound ones...
		if (! unboundDataSets.isEmpty()) {
			Set<DataSet> bound = inputDataSetBindings.stream()
					.map(DataSetBinding::getDataSet)
					.collect(Collectors.toSet());

			unboundDataSets.removeAll(bound);
		}

		// ... and the answer is !
		return unboundDataSets;
	}

	public void setKnobBindings(List<KnobBinding> list) {
		knobBindings = list;
	}
	public void setInputDataSetBindings(List<DataSetBinding> list) {
		inputDataSetBindings = list;
	}
}
