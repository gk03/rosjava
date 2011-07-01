/*
 * Copyright (C) 2011 Google Inc.
 * 
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not
 * use this file except in compliance with the License. You may obtain a copy of
 * the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS, WITHOUT
 * WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied. See the
 * License for the specific language governing permissions and limitations under
 * the License.
 */

package org.ros.internal.node.server;

import com.google.common.base.Preconditions;
import com.google.common.collect.HashMultimap;
import com.google.common.collect.Maps;
import com.google.common.collect.Multimap;
import com.google.common.collect.Multimaps;
import com.google.common.collect.Sets;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.ros.internal.namespace.GraphName;
import org.ros.internal.node.client.SlaveClient;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.Vector;

/**
 * @author damonkohler@google.com (Damon Kohler)
 */
public class ParameterServer {

  private static final Log log = LogFactory.getLog(ParameterServer.class);

  private final Map<String, Object> tree;
  private final Multimap<GraphName, SlaveIdentifier> subscribers;
  private final GraphName masterName;

  public ParameterServer() {
    tree = Maps.newConcurrentMap();
    subscribers = Multimaps.synchronizedMultimap(HashMultimap.<GraphName, SlaveIdentifier>create());
    masterName = new GraphName("/master");
  }

  public void subscribe(GraphName name, SlaveIdentifier slaveIdentifier) {
    subscribers.put(name, slaveIdentifier);
  }

  private Stack<String> getGraphNameParts(GraphName name) {
    Stack<String> parts = new Stack<String>();
    GraphName tip = name;
    while (!tip.isRoot()) {
      parts.add(tip.getBasename().toString());
      tip = tip.getParent();
    }
    return parts;
  }

  @SuppressWarnings("unchecked")
  public Object get(GraphName name) {
    Preconditions.checkArgument(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Object possibleSubtree = tree;
    while (!parts.empty() && possibleSubtree != null) {
      if (!(possibleSubtree instanceof Map)) {
        return null;
      }
      possibleSubtree = ((Map<String, Object>) possibleSubtree).get(parts.pop());
    }
    return possibleSubtree;
  }

  @SuppressWarnings("unchecked")
  private void setValue(GraphName name, Object value) {
    Preconditions.checkArgument(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Map<String, Object> subtree = tree;
    while (!parts.empty()) {
      String part = parts.pop();
      if (parts.empty()) {
        subtree.put(part, value);
      } else if (subtree.containsKey(part) && subtree.get(part) instanceof Map) {
        subtree = (Map<String, Object>) subtree.get(part);
      } else {
        Map<String, Object> newSubtree = Maps.newHashMap();
        subtree.put(part, newSubtree);
        subtree = newSubtree;
      }
    }
  }

  private interface Updater {
    void update(SlaveClient client);
  }

  private <T> void update(GraphName name, T value, Updater updater) {
    setValue(name, value);
    synchronized (subscribers) {
      for (SlaveIdentifier slaveIdentifier : subscribers.get(name)) {
        SlaveClient client = new SlaveClient(masterName, slaveIdentifier.getUri());
        try {
          updater.update(client);
        } catch (Exception e) {
          log.error(e);
        }
      }
    }
  }

  public void set(final GraphName name, final boolean value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final int value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final double value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final String value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final char value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final byte value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final short value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final List<?> value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final Vector<?> value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  public void set(final GraphName name, final Map<?, ?> value) {
    update(name, value, new Updater() {
      @Override
      public void update(SlaveClient client) {
        client.paramUpdate(name.toString(), value);
      }
    });
  }

  @SuppressWarnings("unchecked")
  public void delete(GraphName name) {
    Preconditions.checkArgument(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Map<String, Object> subtree = tree;
    while (!parts.empty() && subtree.containsKey(parts.peek())) {
      String part = parts.pop();
      if (parts.empty()) {
        subtree.remove(part);
      } else {
        subtree = (Map<String, Object>) subtree.get(part);
      }
    }
  }

  public Object search(GraphName name) {
    throw new UnsupportedOperationException();
  }

  @SuppressWarnings("unchecked")
  public boolean has(GraphName name) {
    Preconditions.checkArgument(name.isGlobal());
    Stack<String> parts = getGraphNameParts(name);
    Map<String, Object> subtree = tree;
    while (!parts.empty() && subtree.containsKey(parts.peek())) {
      String part = parts.pop();
      if (!parts.empty()) {
        subtree = (Map<String, Object>) subtree.get(part);
      }
    }
    return parts.empty();
  }

  @SuppressWarnings("unchecked")
  private Set<GraphName> getSubtreeNames(GraphName parent, Map<String, Object> subtree,
      Set<GraphName> names) {
    for (String name : subtree.keySet()) {
      Object possibleSubtree = subtree.get(name);
      if (possibleSubtree instanceof Map) {
        names.addAll(getSubtreeNames(parent.join(new GraphName(name)),
            (Map<String, Object>) possibleSubtree, names));
      } else {
        names.add(parent.join(new GraphName(name)));
      }
    }
    return names;
  }

  public Collection<GraphName> getNames() {
    Set<GraphName> names = Sets.newHashSet();
    return getSubtreeNames(GraphName.createRoot(), tree, names);
  }

}
