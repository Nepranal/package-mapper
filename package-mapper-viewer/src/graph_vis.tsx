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
  const ref = useRef(null);
  const [width, setWidth] = useState(window.innerWidth);
  const [height, setHeight] = useState(window.innerHeight);
  const [minWidth, setMinWidth] = useState(-width / 2);
  const [minHeight, setMinHeight] = useState(-height / 2);

  useEffect(() => {
    const color = d3.scaleOrdinal(types, d3.schemeCategory10);
    const simulation = d3
      .forceSimulation(nodes)
      .force(
        "link",
        d3.forceLink<Node, Graph>(links).id((d) => d.id)
      )
      .force("charge", d3.forceManyBody().strength(-8000))
      .force("x", d3.forceX())
      .force("y", d3.forceY());

    const svg = d3.select(ref.current);
    if (svg.selectAll("g").empty()) {
      svg
        .append("defs")
        .selectAll("marker")
        .data(types)
        .join("marker")
        .attr("id", (d) => `arrow-${d}`)
        .attr("viewBox", "0 -5 10 10")
        .attr("refX", 15)
        .attr("refY", -0.5)
        .attr("markerWidth", 6)
        .attr("markerHeight", 6)
        .attr("orient", "auto")
        .append("path")
        // .attr("fill", color)
        .attr("d", "M0,-5L10,0L0,5");

      const link = svg
        .append("g")
        .attr("fill", "none")
        .attr("stroke-width", 1.5)
        .selectAll("path")
        .data(links)
        .join("path")
        .attr("stroke", "#000000")
        .attr(
          "marker-end",
          (d) => `url(${new URL(`#arrow-${d.type}`, location.toString())})`
        );

      const node = svg
        .append("g")
        .attr("fill", "currentColor")
        .attr("stroke-linecap", "round")
        .attr("stroke-linejoin", "round")
        .selectAll("g")
        .data(nodes)
        .join("g")
        .call(drag(simulation) as any);

      node
        .append("circle")
        .attr("stroke", "white")
        .attr("stroke-width", 1.5)
        .attr("r", 5)
        .attr("color", (d) => color(d.id.split("/").slice(0, -1).join(""))); // Extract file prefix

      node
        .append("text")
        .attr("x", 8)
        .attr("y", "0.31em")
        .text((d) => d.id)
        .clone(true)
        .lower()
        .attr("fill", "none")
        .attr("stroke", "white")
        .attr("stroke-width", 3);

      simulation.on("tick", () => {
        link.attr("d", linkArc);
        node.attr("transform", (d) => `translate(${d.x},${d.y})`);
      });
    }
  }, []);

  const mouseDown = useRef(false);
  return (
    <>
      <div>
        <svg
          ref={ref}
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
        ></svg>
      </div>
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
];
