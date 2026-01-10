import * as d3 from "d3";
import { useEffect, useRef, useState } from "react";
import type { Graph } from "./App";

export interface GraphVisProps {
  types: string[];
  nodes: Node[];
  links: Graph[];
}

interface Node extends d3.SimulationNodeDatum {
  id: string;
}

export default function GraphVis({ types, nodes, links }: GraphVisProps) {
  const [width, setWidth] = useState(window.innerWidth);
  const [height, setHeight] = useState(window.innerHeight);
  const [minWidth, setMinWidth] = useState(-width / 2);
  const [minHeight, setMinHeight] = useState(-height / 2);
  const [color, setColor] = useState(() =>
    d3.scaleOrdinal(types, d3.schemeCategory10)
  );

  const mouseDown = useRef(false);

  useEffect(() => {
    const simulation = d3
      .forceSimulation(nodes)
      .force(
        "link",
        d3.forceLink<Node, Graph>(links).id((d) => d.id)
      )
      .force("charge", d3.forceManyBody().strength(-8000))
      .force("x", d3.forceX())
      .force("y", d3.forceY());

    const link = d3.select(".link").selectAll("path").data(links).join("path");
    const node = d3
      .select(".node")
      .selectAll("g")
      .data(nodes)
      .join("g")
      .call(drag(simulation) as any);

    simulation.on("tick", () => {
      link.attr("d", linkArc);
      node.attr("transform", (d) => `translate(${d.x},${d.y})`);
    });
    setColor(() => d3.scaleOrdinal(types, d3.schemeCategory10));
  }, [types, nodes, links]);

  return (
    <>
      <svg
        width={"100vw"}
        height={"100vh"}
        viewBox={`${minWidth}, ${minHeight}, ${width}, ${height}`}
        style={{ maxWidth: "100%", font: "12px sans-serif" }}
        onWheel={(e) => {
          const pt = transform2D(
            { x: e.clientX, y: e.clientY },
            { x: minWidth, y: minHeight },
            { x: width / window.innerWidth, y: height / window.innerHeight }
          );
          const increment = e.deltaY > 0 ? 10 : -10;
          const newWidth = width + increment;
          const newHeight = height + increment;
          const ptTransformed = transform2D(
            { x: e.clientX, y: e.clientY },
            { x: minWidth, y: minHeight },
            {
              x: newWidth / window.innerWidth,
              y: newHeight / window.innerHeight,
            }
          );

          setWidth((_) => newWidth);
          setHeight((_) => newHeight);
          setMinWidth((w) => w - (ptTransformed.x - pt.x));
          setMinHeight((h) => h - (ptTransformed.y - pt.y));
        }}
        onMouseDown={() => {
          mouseDown.current = true;
        }}
        onMouseUp={() => {
          mouseDown.current = false;
        }}
        onMouseMove={(e) => {
          if (mouseDown.current) {
            setMinWidth((w) => w - e.movementX);
            setMinHeight((h) => h - e.movementY);
          }
        }}
      >
        <defs>
          <marker
            id={`arrow-import`}
            viewBox="0 -5 10 10"
            refX={15}
            refY={-0.5}
            markerWidth={6}
            markerHeight={6}
            orient={"auto"}
          >
            <path d="M0,-5L10,0L0,5"></path>
          </marker>
        </defs>
        {/* Links */}
        <g fill="None" strokeWidth={1.5} className="link">
          {links.map((l, i) => (
            <path
              key={i}
              stroke="#000000"
              markerEnd={(() => {
                return `url(${new URL(
                  `#arrow-${l.type}`,
                  location.toString()
                )})`;
              })()}
            ></path>
          ))}
        </g>
        {/* Nodes */}
        <g
          fill="currentColor"
          strokeLinecap="round"
          strokeLinejoin="round"
          className="node"
        >
          {nodes.map((n) => (
            <g key={n.id}>
              <text
                x={8}
                y={"0.31em"}
                stroke="white"
                strokeWidth={3}
                fill="none"
              >
                {n.id}
              </text>
              <circle
                stroke="white"
                strokeWidth={1.5}
                r={5}
                color={color(n.id.split("/").slice(0, -1).join(""))}
              ></circle>
              <text x={8} y={"0.31em"}>
                {n.id}
              </text>
            </g>
          ))}
        </g>
      </svg>
    </>
  );
}

// Helper functions

const drag = (simulation: d3.Simulation<Node, undefined>) => {
  function dragstarted(event: any, d: any) {
    if (!event.active) simulation.alphaTarget(0.3).restart();
    d.fx = d.x;
    d.fy = d.y;
  }

  function dragged(event: any, d: any) {
    d.fx = event.x;
    d.fy = event.y;
  }

  function dragended(event: any, d: any) {
    if (!event.active) simulation.alphaTarget(0);
    d.fx = null;
    d.fy = null;
  }

  return d3
    .drag()
    .on("start", dragstarted)
    .on("drag", dragged)
    .on("end", dragended);
};

const transform2D = (
  point: { x: number; y: number },
  translate: { x: number; y: number },
  scale: { x: number; y: number }
) => {
  return {
    x: point.x * scale.x + translate.x,
    y: point.y * scale.y + translate.y,
  };
};

function linkArc(d: any) {
  const r = Math.hypot(d.target.x - d.source.x, d.target.y - d.source.y);
  return `
    M${d.source.x},${d.source.y}
    A${r},${r} 0 0,1 ${d.target.x},${d.target.y}
  `;
}

// Data

export const graph: Graph[] = [
  {
    source: "chained-reconstructions/DataService.py",
    target: "chained-reconstructions/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/DataService.py",
    target: "chained-reconstructions/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/DataService.py",
    target: "chained-reconstructions/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/ChainReconstructionService.py",
    target: "chained-reconstructions/DataService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/app.py",
    target: "chained-reconstructions/DataService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/DataService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/AlignmentService.py",
    target: "chained-reconstructions/DataService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/ChainReconstructionService.py",
    target: "chained-reconstructions/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/app.py",
    target: "chained-reconstructions/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/AlignmentService.py",
    target: "chained-reconstructions/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/DBService.py",
    target: "chained-reconstructions/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/ChainReconstructionService.py",
    target: "chained-reconstructions/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/app.py",
    target: "chained-reconstructions/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/ChainReconstructionService.py",
    target: "chained-reconstructions/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/app.py",
    target: "chained-reconstructions/DBService.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.pack",
    target: "chained-reconstructions/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/AlignmentService.py",
    target: "chained-reconstructions/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/ChainReconstructionService.py",
    target: "chained-reconstructions/utils.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/app.py",
    target: "chained-reconstructions/ChainReconstructionService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/ChainReconstructionService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/ChainReconstructionService.py",
    target: "chained-reconstructions/AlignmentService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/utils.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/AlignmentService.py",
    target: "chained-reconstructions/utils.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.pack",
    target: "chained-reconstructions/app.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/app.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/app.py",
    target: "chained-reconstructions/AlignmentService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/AlignmentService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/ORIG_HEAD",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/config",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.pack",
    target: "chained-reconstructions/chained_colmap/DBService.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.pack",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.pack",
    target: "chained-reconstructions/chained_colmap/app.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.idx",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/HEAD",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/logs/HEAD",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/logs/refs/heads/main",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/logs/refs/remotes/origin/HEAD",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/refs/heads/main",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/refs/remotes/origin/HEAD",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/refs/remotes/origin/main",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/DataService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/utils.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/app.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/chained_colmap/AlignmentService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/FETCH_HEAD",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/ORIG_HEAD",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/config",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.pack",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/.git/objects/pack/pack-fe93bbeebfa3cf36ddb4db60ecb09eba836ebdfb.idx",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/HEAD",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/logs/HEAD",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/logs/refs/heads/main",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/logs/refs/remotes/origin/HEAD",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/refs/heads/main",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/refs/remotes/origin/HEAD",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/refs/remotes/origin/main",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/index",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.git/FETCH_HEAD",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DataService.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/FileService.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/database.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DBService.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/.gitignore",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/utils.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/AlignmentService.py",
    target: "chained-reconstructions/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DataService.py",
    target: "chained-reconstructions/chained_colmap/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DataService.py",
    target: "chained-reconstructions/chained_colmap/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DataService.py",
    target: "chained-reconstructions/chained_colmap/DBService.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/chained_colmap/DataService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DataService.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/chained_colmap/DataService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/AlignmentService.py",
    target: "chained-reconstructions/chained_colmap/DataService.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/chained_colmap/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/FileService.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/chained_colmap/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/AlignmentService.py",
    target: "chained-reconstructions/chained_colmap/FileService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DBService.py",
    target: "chained-reconstructions/chained_colmap/database.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/chained_colmap/database.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/database.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/chained_colmap/database.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/chained_colmap/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/DBService.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/chained_colmap/DBService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/AlignmentService.py",
    target: "chained-reconstructions/chained_colmap/DBService.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/chained_colmap/utils.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    type: "import",
  },
  {
    source:
      "chained-reconstructions/chained_colmap/ChainReconstructionService.py",
    target: "chained-reconstructions/chained_colmap/AlignmentService.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/utils.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/AlignmentService.py",
    target: "chained-reconstructions/chained_colmap/.gitignore",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/chained_colmap/utils.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/AlignmentService.py",
    target: "chained-reconstructions/chained_colmap/utils.py",
    type: "import",
  },
  {
    source: "chained-reconstructions/chained_colmap/app.py",
    target: "chained-reconstructions/chained_colmap/AlignmentService.py",
    type: "import",
  },
];
