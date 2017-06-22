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
package com.diversityarrays.kdxplore.trialdesign;

import java.awt.Point;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import javax.swing.event.TableModelEvent;
import javax.swing.event.TableModelListener;

import com.diversityarrays.kdxplore.Shared;
import com.diversityarrays.kdxplore.design.EntryCountChangeListener;
import com.diversityarrays.kdxplore.design.EntryType;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.PlantingBlockChangeListener;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlock.WhatChanged;
import com.diversityarrays.kdxplore.fieldlayout.PlantingBlockTableModel;
import com.diversityarrays.kdxplore.fieldlayout.ReplicateCellContent;
import com.diversityarrays.kdxplore.fieldlayout.SiteLocation;
import com.diversityarrays.util.Check;
import com.diversityarrays.util.TableColumnInfo;
import com.diversityarrays.util.TableColumnInfoTableModel;
import com.diversityarrays.util.XYPos;

public class PositionedTrialEntryTableModel extends TableColumnInfoTableModel<PositionedDesignEntry<TrialEntry>> {

	private final Set<Integer> selectedReplicateNumbers = new HashSet<>();

	private Integer plotTypeColumnIndex = null;
	private Map<TrialHeading, String> userHeadings;

	private final TrialEntryFile trialEntriesInfo;

	private Map<Integer, PlantingBlock<?>> blockByReplicateNumber;

	private Map<Integer, List<PositionedDesignEntry<TrialEntry>>> positionedEntriesByReplicate = new HashMap<>();

	private final SiteLocation siteLocation;

	private PlantingBlockChangeListener<ReplicateCellContent> onBlockChanged = new PlantingBlockChangeListener<ReplicateCellContent>() {
		@Override
		public void blockChanged(PlantingBlock<ReplicateCellContent> pb,
				WhatChanged whatChanged,
				Map<Point, ReplicateCellContent> oldContentByPoint)
		{
			switch (whatChanged) {
			case BORDER:
				break;
			case CONTENT:
			case DIMENSION:
			case ENTRY_TYPES:
			case ORIGIN:
			case POSITION:
				updatePositionedDataFor(pb, true);
				break;

			case MINIMUM_CELL_COUNT:
			case SPATIALS_REQUIRED:
				break;
			default:
				break;
			}
		}
	};

	private final PlantingBlockTableModel<ReplicateCellContent> plantingBlockTableModel;

	public PositionedTrialEntryTableModel(SiteLocation location,
			PlantingBlockTableModel<ReplicateCellContent> pbtm,
			TrialEntryFile tef)
	{
		this.siteLocation = location;
		this.trialEntriesInfo = tef;
		this.userHeadings = trialEntriesInfo.getUserHeadings();

		this.plantingBlockTableModel = pbtm;
		this.plantingBlockTableModel.addTableModelListener(new TableModelListener() {
			@Override
			public void tableChanged(TableModelEvent e) {
				if (TableModelEvent.ALL_COLUMNS == e.getColumn()) {
					updatePlantingBlocks();
				}
			}
		});
		updatePlantingBlocks();
	}

	private void updatePlantingBlocks() {
		List<PlantingBlock<ReplicateCellContent>> blocks = plantingBlockTableModel.getPlantingBlocks();
		this.blockByReplicateNumber = blocks.stream()
				.collect(Collectors.toMap(b -> b.getReplicateNumber(), Function.identity()));

		blocks.forEach(pb -> pb.addPlantingBlockChangeListener(onBlockChanged));

		for (Integer rep : blockByReplicateNumber.keySet()) {
			List<PositionedDesignEntry<TrialEntry>> list = trialEntriesInfo.getEntries().stream()
					.map(te -> new PositionedDesignEntry<>(rep,
							null/* where (XYPos) */, null /* block */, te))
					.collect(Collectors.toList());
			positionedEntriesByReplicate.put(rep, list);
		}
		initWithTrialEntriesInfo(blockByReplicateNumber.keySet());
	}

	private void initWithTrialEntriesInfo(Set<Integer> replicates) {
		this.plotTypeColumnIndex = null;

		List<TableColumnInfo<PositionedDesignEntry<TrialEntry>>> entryInfos = new ArrayList<>();

		// String entryIdHeading = userHeadings.get(TrialHeading.ENTRY_ID);
		// if (Check.isEmpty(entryIdHeading)) {
		// entryIdHeading = TrialHeading.ENTRY_ID.display;
		// }

		entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>("Rep#", Integer.class) {
			@Override
			public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> pde) {
				return pde.getReplicateNumber();
			}
		});

		String plotTypeHeading = userHeadings.get(TrialHeading.ENTRY_TYPE);
		if (Check.isEmpty(plotTypeHeading)) {
			plotTypeColumnIndex = null;
		} else {
			plotTypeColumnIndex = entryInfos.size();

			entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>(plotTypeHeading, EntryType.class) {
				@Override
				public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> pde) {
					return pde.getEntryType();
				}
			});
		}

		Optional<PositionedDesignEntry<TrialEntry>> optAnyBlocks = positionedEntriesByReplicate.values().stream()
				.flatMap(list -> list.stream()).filter(pde -> pde.getBlock() != null).findFirst();

		if (optAnyBlocks.isPresent()) {
			entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>("Block", Integer.class) {
				@Override
				public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> pde) {
					return pde.getBlock();
				}
			});
		}

		entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>("X", Integer.class) {
			@Override
			public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> pde) {
				Optional<XYPos> where = pde.getWhere();
				if (where.isPresent()) {
					PlantingBlock<?> pb = blockByReplicateNumber.get(pde.getReplicateNumber());
					if (pb != null) {
						int x = pb.getUserXcoord(where.get().x);
						return siteLocation.getUserXcoord(x);
					}
				}
				return null;
			}
		});
		entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>("Y", Integer.class) {
			@Override
			public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> pde) {
				Optional<XYPos> where = pde.getWhere();
				if (where.isPresent()) {
					PlantingBlock<?> pb = blockByReplicateNumber.get(pde.getReplicateNumber());
					if (pb != null) {
						int y = pb.getUserYcoord(where.get().y);
						return siteLocation.getUserYcoord(y);
					}
				}
				return null;
			}
		});

		BiConsumer<TrialHeading, String> addForHeading = new BiConsumer<TrialHeading, String>() {
			@Override
			public void accept(TrialHeading th, String userHeading) {
				switch (th) {
				case DONT_USE:
					break;
				case ENTRY_TYPE:
					// already done (optional second column)
					break;
				case FACTOR:
					// done separately
					break;

				case ENTRY_ID:
					entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>(userHeading, Integer.class) {
						@Override
						public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> pde) {
							return pde.getEntryId();
						}
					});
					break;

				case ENTRY_NAME:
					entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>(userHeading, String.class) {
						@Override
						public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> te) {
							return te.getEntryName();
						}
					});
					break;

				case EXPERIMENT:
					entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>(userHeading, String.class) {
						@Override
						public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> te) {
							return te.getNurseryOrExperiment();
						}
					});
					break;

				case LOCATION:
					entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>(userHeading, String.class) {
						@Override
						public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> te) {
							return te.getLocation();
						}
					});
					break;

				case NESTING:
					entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>(userHeading, String.class) {
						@Override
						public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> te) {
							return te.getEntry().getNesting();
						}
					});
					break;

				default:
					entryInfos.add(new TableColumnInfo<PositionedDesignEntry<TrialEntry>>(userHeading, String.class) {
						@Override
						public Object getColumnValue(int rowIndex, PositionedDesignEntry<TrialEntry> te) {
							return "?unsupported TrialHeading: " + th;
						}
					});
					break;
				}
			}
		};

		userHeadings.entrySet().stream().forEach(e -> addForHeading.accept(e.getKey(), e.getValue()));

		setTableColumnInfo(entryInfos);

		setSelectedReplicates(replicates, false);

		fireTableStructureChanged();
		fireEntryCountChanged();
	}

	public void setSelectedBlocks(Collection<PlantingBlock<ReplicateCellContent>> blocks) {
		Set<Integer> replicateNumbers = blocks.stream()
			.map(PlantingBlock::getReplicateNumber)
			.collect(Collectors.toSet());

		setSelectedReplicates(replicateNumbers, true);

		updatePositionedDataForThese(blocks);
	}

//	public void setSelectedReplicates(Collection<Integer> reps) {
//		setSelectedReplicates(reps, true);
//	}

	private void setSelectedReplicates(Collection<Integer> reps, boolean fire) {
		selectedReplicateNumbers.clear();
		selectedReplicateNumbers.addAll(reps);

		if (trialEntriesInfo == null) {
			setItems(Collections.emptyList());
		} else {
			refreshItems();
		}
	}

	private void refreshItems() {
		List<PositionedDesignEntry<TrialEntry>> list = selectedReplicateNumbers.stream()
				.map(rep -> positionedEntriesByReplicate.get(rep))
				.filter(Objects::nonNull)
				.flatMap(List::stream)
				.collect(Collectors.toList());
		setItems(list);
	}

	public void addEntryCountChangeListener(EntryCountChangeListener l) {
		listenerList.add(EntryCountChangeListener.class, l);
	}

	public void removeEntryCountChangeListener(EntryCountChangeListener l) {
		listenerList.remove(EntryCountChangeListener.class, l);
	}

	protected void fireEntryCountChanged() {
		for (EntryCountChangeListener l : listenerList.getListeners(EntryCountChangeListener.class)) {
			l.entryCountChanged(this);
		}
	}

	private void updatePositionedDataForThese(Collection<PlantingBlock<ReplicateCellContent>> changedBlocks) {

		Map<Integer, PlantingBlock<ReplicateCellContent>> blockByReplicateNumber = changedBlocks.stream()
				.collect(Collectors.toMap(PlantingBlock::getReplicateNumber, Function.identity()));

		boolean anyChanges = false;
		for (Integer rep : selectedReplicateNumbers) {

			PlantingBlock<ReplicateCellContent> pb = blockByReplicateNumber.get(rep);
			if (pb == null || pb.isEmpty()) {
				continue;
			}

			if (updatePositionedDataFor(pb, false)) {
				anyChanges = true;
			}
		}

		if (anyChanges) {
			refreshItems();
		}
	}

	private boolean updatePositionedDataFor(PlantingBlock<ReplicateCellContent> pb, boolean doRefresh) {

		int replicateNumber = pb.getReplicateNumber();

		List<PositionedDesignEntry<TrialEntry>> positionedTrialEntries = positionedEntriesByReplicate.get(replicateNumber);
		if (! Check.isEmpty(positionedTrialEntries)) {
			return false;
		}

		boolean anyChanges = false;

		// Map<Integer, PositionedDesignEntry<TrialEntry>> pdeBySequence =
		// positionedTrialEntries.stream()
		// .collect(Collectors.toMap(pde ->
		// Integer.valueOf(pde.getSequence()), Function.identity()));

		Map<Integer, PositionedDesignEntry<TrialEntry>> pdeBySequence = new HashMap<>();
		for (PositionedDesignEntry<TrialEntry> pde : positionedTrialEntries) {
			int sequence = pde.getSequence();
			while (pdeBySequence.get(sequence) != null) {
				sequence++;
			}
			pdeBySequence.put(sequence, pde);
		}

		List<PositionedDesignEntry<TrialEntry>> newList = new ArrayList<>();

		Map<Point, ReplicateCellContent> cbyp = pb.getContentByPoint();

		for (Point pt : cbyp.keySet()) {
			ReplicateCellContent rcc = cbyp.get(pt);

			TrialEntry trialEntry = rcc.trialEntry;
			PositionedDesignEntry<TrialEntry> pde = pdeBySequence.get(trialEntry.getSequence());
			if (pde == null) {
				Shared.Log.w(this.getClass().getSimpleName(),
						"Missing PDE[" + trialEntry.getSequence() + "] at " + pt.x + "," + pt.y);
			} else {
				PositionedDesignEntry<TrialEntry> copy = pde.makeCopyReplacing(trialEntry, pt.x, pt.y);
				newList.add(copy);
				pdeBySequence.put(trialEntry.getSequence(), copy);
				anyChanges = true;
			}
		}

		positionedEntriesByReplicate.put(replicateNumber, newList);

		if (anyChanges && doRefresh) {
			refreshItems();
		}

		return anyChanges;
	}

}
