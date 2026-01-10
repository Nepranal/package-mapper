import { useRef, useState } from "react";
import "./App.css";
import GraphVis, { graph as data } from "./graph_vis";
import OtherVis from "./other_vis/other_vis";
import { GraphService } from "./graph_service";
import toast from "react-hot-toast";

export interface Publisher {
  subscribers: Subscriber[];

  notify: (repoName: string, version: string) => void;
  addSubscriber: (s: Subscriber) => void;
  removeSubscriber: (index: number) => void;
}

export interface Subscriber {
  update: (repoName: string, version: string) => void;
}

export interface Graph {
  source: string;
  target: string;
  type: string;
}

function App() {
  const [graph, setGraph] = useState(data);

  const graphService = useRef<GraphService>(null);
  graphService.current = graphService.current ?? new GraphService();

  const publisher = useRef<Publisher>(undefined);
  publisher.current = publisher.current ?? {
    subscribers: [
      {
        async update(repoName, version) {
          setGraph(
            await toast.promise(
              graphService.current!.getGraph(repoName, version),
              {
                loading: "Fetching graph...",
                success: <b>Fetched!</b>,
                error: (err) => `Couldn't fetch: ${err}`,
              }
            )
          );
        },
      },
    ],
    notify(repoName, version) {
      for (const s of this.subscribers) {
        s.update(repoName, version);
      }
    },
    removeSubscriber(i) {
      this.subscribers = [
        ...this.subscribers.slice(0, i),
        ...this.subscribers.slice(i + 1),
      ];
    },
    addSubscriber(s) {
      this.subscribers.push(s);
    },
  };

  return (
    <>
      <div id="main">
        <div id="graph-vis">
          <GraphVis
            types={Array.from(
              new Set(
                graph.flatMap((l) => [
                  l.source.split("/").slice(0, -1).join(""),
                  l.target.split("/").slice(0, -1).join(""),
                ])
              ),
              (id) => id
            )}
            nodes={Array.from(
              new Set(graph.flatMap((l) => [l.source, l.target])),
              (id) => ({ id })
            )}
            links={JSON.parse(JSON.stringify(graph))}
          />
        </div>
        <div id="other-vis">
          <OtherVis publisher={publisher.current} />
        </div>
      </div>
    </>
  );
}

export default App;
