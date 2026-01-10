import axios from "axios";
import type { Graph } from "./App";

export class GraphService {
  readonly BASE_URL = "http://localhost:8080";

  async getGraph(repo: string, version: string) {
    return (
      await axios.get<Graph[]>(
        `${this.BASE_URL}/analyse/graph?repo=${repo}&version=${version}`
      )
    ).data;
  }
}
