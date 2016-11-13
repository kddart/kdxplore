/*
    KDXplore provides KDDart Data Exploration and Management
    Copyright (C) 2015,2016  Diversity Arrays Technology, Pty Ltd.

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
package com.diversityarrays.kdxplore.prefs;

import java.awt.BorderLayout;
import java.awt.CardLayout;
import java.awt.Color;
import java.awt.Font;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JSplitPane;
import javax.swing.JTree;
import javax.swing.event.TreeSelectionEvent;
import javax.swing.event.TreeSelectionListener;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;
import javax.swing.tree.TreeNode;
import javax.swing.tree.TreePath;

import net.pearcan.util.StringUtil;

public class PreferenceTreePanel extends JPanel {

	static class NamedTreeNode extends DefaultMutableTreeNode {
		
		public final String name;
		public NamedTreeNode(String n) {
			name = n;
		}
		
		@Override
		public String toString() {
			return userObject==null ? name : userObject.toString();
		}
		
		@Override
		public boolean isLeaf() {
			boolean b = userObject != null;
			return b;
		}

		public void setPreference(KdxPreference<?> pref) {
			setUserObject(pref);
		}
		
		public KdxPreference<?> getPreference() {
			return (KdxPreference<?>) getUserObject();
		}

		public List<KdxPreference<?>> getChildPreferences() {
			List<KdxPreference<?>> result = new ArrayList<>();
			int nc = getChildCount();
			for (int i = 0; i < nc; ++i) {
				NamedTreeNode child = (NamedTreeNode) getChildAt(i);
				KdxPreference<?> pref = child.getPreference();
				if (pref != null) {
					result.add(pref);
				}
			}
			return result;
		}
	}
	
	
	static public NamedTreeNode buildTree(String rootName) {
		
		NamedTreeNode root = new NamedTreeNode(rootName);
		
		Set<String> paths = new HashSet<>();
		
	    KdxplorePreferences kdxPrefs = KdxplorePreferences.getInstance();

	    List<PreferenceCollection> prefColls = kdxPrefs.getPreferenceCollections();
	    
	    for (PreferenceCollection prefcoll : prefColls) {

	    	String collKey = prefcoll.appKey + "/";
	        for (KdxPreference<?> pref : prefcoll.getKdxPreferences()) {

//	        	System.out.println(collKey + "\t" + pref.key + "=" + pref.getName());
	        	
	        	String pref_key = collKey + pref.key;
	            String[] keyParts = pref_key.split("/"); //$NON-NLS-1$
	            
	            if (paths.add(pref_key)) {
	                List<String> path = new ArrayList<>();
	                for (String kp : keyParts) {
	                    
	                    String name = prefcoll.getPathComponentName(kp);
	                    if (name == null) {
	                        name = kp.toUpperCase();
	                    }
	                    path.add(name);
	                }
	                
	                NamedTreeNode findNode = findNode(root, path);
	                if (findNode.getUserObject() != null) {
	                    throw new RuntimeException("Already an object at " + pref_key+ ": "+ findNode.getUserObject()); //$NON-NLS-1$ //$NON-NLS-2$
	                }
	                findNode.setPreference(pref);
	            }
	            else {
	                throw new RuntimeException("Duplicate preference key: " + pref_key); //$NON-NLS-1$
	            }
	        }
	    }
//		KdxPreference<?>[] values = KdxPreference.values();
//        for (KdxPreference<?> pref : values) {
//            String[] keyParts = pref.key.split("/"); //$NON-NLS-1$
//            if (! kdxPrefs.isSectionSupported(keyParts[0])) {
//                continue;
//            }
//            
//            
//			if (paths.add(pref.key)) {
//				List<String> path = new ArrayList<>();
//				for (String kp : keyParts) {
//					String name = KdxplorePreferences.BRANCH_NAME_BY_PATH_COMPONENT.get(kp);
//					if (name == null) {
//						name = kp.toUpperCase();
//					}
//					path.add(name);
//				}
//				
//				NamedTreeNode findNode = findNode(root, path);
//				if (findNode.getUserObject() != null) {
//					throw new RuntimeException("Already an object at " + pref.key+ ": "+ findNode.getUserObject()); //$NON-NLS-1$ //$NON-NLS-2$
//				}
//				findNode.setPreference(pref);
//			}
//			else {
//				throw new RuntimeException("Duplicate preference key: " + pref.key); //$NON-NLS-1$
//			}
//		}
		
		return root;
	}
	
	static private NamedTreeNode findNode(NamedTreeNode node, List<String> path) {
		
		String lookingFor = path.get(0);
		
		NamedTreeNode found = null;
		for (int childIndex = node.getChildCount(); --childIndex >= 0; ) {
			NamedTreeNode childNode = (NamedTreeNode) node.getChildAt(childIndex);
			if (lookingFor.equals(childNode.name)) {
				found = childNode;
				break;
			}
		}
		if (found == null) {
			// Need to create a new one
			switch (path.size()) {
			case 0:
				throw new IllegalStateException("Coding error"); //$NON-NLS-1$
			case 1:
				// doing it now!
				NamedTreeNode newNode = new NamedTreeNode(lookingFor);
				node.add(newNode);
				return newNode;
			case 2:
			default:
				found = new NamedTreeNode(lookingFor);
				node.add(found);
				break;
			}
		}
		else {
			if (path.size() == 1) {
				return found;
			}
		}
		return findNode(found, path.subList(1, path.size()));
	}
	
	static private void printTree(TreeNode node, int depth) {
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("%02d", depth)); //$NON-NLS-1$
		for (int i = depth*2; --i >= 0; ) {
			sb.append('.');
		}
		String indent = sb.toString();
		System.out.print(indent);
		
		System.out.println(node.toString());
		int nc = node.getChildCount();
		for (int i = 0; i < nc; ++i) {
			printTree(node.getChildAt(i), depth + 1);
		}
		
	}
	
	private final NamedTreeNode root = buildTree("Preferences"); //$NON-NLS-1$
	private final DefaultTreeModel treeModel = new DefaultTreeModel(root);
	
	private final JTree tree = new JTree(treeModel);
	private TreeSelectionListener selectionListener = new TreeSelectionListener() {
		@Override
		public void valueChanged(TreeSelectionEvent e) {
			TreePath treePath = e.getPath();
			Object lpc = treePath.getLastPathComponent();
			if (lpc instanceof NamedTreeNode) {
				NamedTreeNode node = (NamedTreeNode) lpc;
				if (node.isLeaf()) {
					KdxPreference<?> pref = node.getPreference();
					showSinglePreference(pref);
				}
				else {
					List<KdxPreference<?>> pagePrefs = node.getChildPreferences();
					showPageOfPreferences(node.name, pagePrefs);
				}
			}
		}
	};
	
	
	static private final String CARD_NO_PREFS = "PREF0"; //$NON-NLS-1$
	static private final String CARD_ONE_PREF = "PREF1"; //$NON-NLS-1$
	static private final String CARD_N_PREFS = "PREFN"; //$NON-NLS-1$
	
	private KdxplorePreferences preferences = KdxplorePreferences.getInstance();
	
	private final JLabel noPrefs = new JLabel(Msg.LABEL_SELECT_PREF());
	private final SinglePrefPanel singlePrefPanel = new SinglePrefPanel(preferences);
	private final MultiPrefPanel multiPrefPanel = new MultiPrefPanel();

	private final CardLayout cardLayout = new CardLayout();
	private final JPanel cardPanel = new JPanel(cardLayout);
	private final JSplitPane splitPane;
	
	
	public PreferenceTreePanel() {
		super(new BorderLayout());
		
		printTree(root, 0);
		
		cardPanel.add(CARD_NO_PREFS, noPrefs);
		cardPanel.add(CARD_ONE_PREF, singlePrefPanel);
		cardPanel.add(CARD_N_PREFS, multiPrefPanel);
		
		TreeNode[] nodes = treeModel.getPathToRoot(root);
		TreePath path = new TreePath(nodes);
		tree.expandPath(path);
		
		splitPane = new JSplitPane(JSplitPane.HORIZONTAL_SPLIT, new JScrollPane(tree), cardPanel);
		splitPane.setResizeWeight(0.4);
		add(splitPane, BorderLayout.CENTER);
		
		tree.getSelectionModel().addTreeSelectionListener(selectionListener);
	}
	
    public void setInitialPreference(KdxPreference<?> pref) {
        List<String> list = Arrays.asList(pref.key.split("/")); //$NON-NLS-1$
        if (list.size() > 1) {
            list = list.subList(0, list.size()-1);
            NamedTreeNode found = findNode(root, list);
            if (found != null) {
                TreeNode[] pathToRoot = treeModel.getPathToRoot(found);
                tree.expandPath(new TreePath(pathToRoot));
            }
        }
    }


	protected void showPageOfPreferences(String name, List<KdxPreference<?>> pagePrefs) {
		if (pagePrefs.isEmpty()) {
			cardLayout.show(cardPanel, CARD_NO_PREFS);
		}
		else {
			multiPrefPanel.setPreferences(name, pagePrefs);
			cardLayout.show(cardPanel, CARD_N_PREFS);
		}
	}

	protected void showSinglePreference(KdxPreference<?> pref) {
		singlePrefPanel.setPreference(pref);
		cardLayout.show(cardPanel, CARD_ONE_PREF);
	}
	
	class MultiPrefPanel extends JPanel {
		
		private final JLabel label = new JLabel();
		private final JLabel textArea = new JLabel();
		
		MultiPrefPanel() {
			super(new BorderLayout());
			label.setFont(label.getFont().deriveFont(Font.BOLD));
			add(label, BorderLayout.NORTH);
			add(textArea, BorderLayout.CENTER);
//			add(new JScrollPane(textArea), BorderLayout.CENTER);
		}

		public void setPreferences(String name, List<KdxPreference<?>> pagePrefs) {
			
			label.setText(name);
			
			String text = pagePrefs.stream()
			    .map(pref -> htmlEscape(pref))
			    .collect(Collectors.joining("",  //$NON-NLS-1$
			            "<html><table border='1'>",  //$NON-NLS-1$
			            "</table>")); //$NON-NLS-1$
			textArea.setText(text);
		}
		
		private String htmlEscape(KdxPreference<?> pref) {
		    
		    String title = Msg.getMessageIdText(pref.messageId);
		    
		    Object value = preferences.getPreferenceValue(pref);
		    StringBuilder sb = new StringBuilder("<tr><td>"); //$NON-NLS-1$
            sb.append(StringUtil.htmlEscape(title))
                .append("</td>") //$NON-NLS-1$
                ;
            if (value != null) {
                if (value instanceof Color) {
                    Color color = (Color) value;
                    sb.append(String.format("<td style='background-color: #%06x'>&nbsp;</td>",  //$NON-NLS-1$
                            color.getRGB() & 0xffffff));
                }
                else {
                    sb.append("<td>") //$NON-NLS-1$
                        .append(StringUtil.htmlEscape(value.toString()))
                        .append("</td>"); //$NON-NLS-1$
                }
            }
            sb.append("</tr>"); //$NON-NLS-1$
            return sb.toString();
		}
	}


	public void doPostOpenActions() {
		splitPane.setDividerLocation(splitPane.getResizeWeight());		
	}


}
