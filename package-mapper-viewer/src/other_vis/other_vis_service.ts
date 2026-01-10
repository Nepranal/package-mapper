import axios from "axios";

export class OtherVisService {
  readonly BASE_URL = "http://localhost:8080";

  async fetchRepos() {
    const d = await axios.get<string[]>(`${this.BASE_URL}/repository/all`);
    return d.data;
  }

  async updateRepoFn(repo: string) {
    return (
      await axios.put<string[]>(
        `${this.BASE_URL}/repository/fetch?repo=${repo}`
      )
    ).data;
  }

  async getVersionListFn(repo: string) {
    const d = await axios.get<string[]>(
      `${this.BASE_URL}/repository/log?repo=${repo}`
    );
    return d.data;
  }

  async downloadRepository(url: string) {
    const d = await axios.post<void>(`${this.BASE_URL}/repository/download`, {
      url,
    });
    return d.data;
  }
}
