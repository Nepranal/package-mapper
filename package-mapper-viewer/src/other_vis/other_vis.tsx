import { useEffect, useRef, useState } from "react";
import Autocomplete from "@mui/material/Autocomplete";
import TextField from "@mui/material/TextField";
import toast from "react-hot-toast";
import type { Publisher } from "../App";
import Button from "@mui/material/Button";
import { OtherVisService } from "./other_vis_service";

interface BoxProps<T = string> {
  valueList: string[];
  cb: (val: T | null) => void;
  label: string;
  val: string;
  style?: React.CSSProperties;
}

function WritableDropDownBox({ label, valueList, cb, val, style }: BoxProps) {
  return (
    <Autocomplete
      onChange={(_, value) => cb(value)}
      renderInput={(params) => <TextField {...params} label={label} />}
      options={valueList}
      value={val}
      clearOnBlur
      freeSolo
      handleHomeEndKeys
      style={style}
    />
  );
}

export default function OtherVis({ publisher }: { publisher?: Publisher }) {
  const [version, setVersion] = useState<string | null>();
  const [repo, setRepo] = useState<string | null>();
  const [versionList, setVersionList] = useState<string[]>([]);
  const [repoList, setRepoList] = useState<string[]>([]);
  const [url, setUrl] = useState<string | null>();
  const [downloading, setDownloading] = useState(false);
  const [updating, setUpdating] = useState(false);

  const otherVisService = useRef<OtherVisService>(null);
  otherVisService.current = otherVisService.current ?? new OtherVisService();

  const fetchRepos = async () =>
    setRepoList(
      await toast.promise(otherVisService.current!.fetchRepos(), {
        loading: "Fetching repository names...",
        success: <b>Fetched!</b>,
        error: (err) => `Couldn't fetch: ${err}`,
      })
    );
  const updateRepoFn = async (repo: string) =>
    setVersionList(
      await toast.promise(otherVisService.current!.updateRepoFn(repo), {
        loading: "Updating repository...",
        success: <b>Updated!</b>,
        error: (err) => `Couldn't update: ${err}`,
      })
    );
  const getVersionListFn = async (repo: string) =>
    setVersionList(
      await toast.promise(otherVisService.current!.getVersionListFn(repo), {
        loading: "Getting commit versions...",
        success: <b>Fetched!</b>,
        error: (err) => `Couldn't fetch: ${err}`,
      })
    );

  // Use effects
  useEffect(() => {
    fetchRepos();
  }, []);
  useEffect(() => {
    if (repo) {
      getVersionListFn(repo);
    } else {
      setVersionList([]);
      setVersion(null);
    }
  }, [repo]);
  useEffect(() => {
    if (repo && version) {
      publisher?.notify(repo, version);
    }
  }, [version]);

  return (
    <>
      <WritableDropDownBox
        valueList={versionList}
        cb={(val) => setVersion(val)}
        label="Commit version"
        val={version ?? ""}
        style={{ margin: "auto" }}
      />
      <div style={{ display: "flex", margin: "10px 0px", gap: "2px" }}>
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          <WritableDropDownBox
            valueList={repoList}
            cb={(val) => setRepo(val)}
            label="Repository name"
            val={repo ?? ""}
            style={{ flexGrow: 1 }}
          />
          <TextField
            id="outlined-basic"
            label="Repository URL"
            variant="outlined"
            style={{ flexGrow: 1 }}
            onChange={(e) =>
              setUrl(e.target.value === "" ? null : e.target.value)
            }
            value={url ?? ""}
          />
        </div>
        <div style={{ display: "flex", flexDirection: "column", gap: "10px" }}>
          <Button
            variant="outlined"
            onClick={async () => {
              setUpdating(true);
              await updateRepoFn(repo!);
              setUpdating(false);
            }}
            style={{ flexGrow: 1 }}
            disabled={updating || repo == null}
          >
            Update
          </Button>
          <Button
            variant="outlined"
            style={{ flexGrow: 1 }}
            disabled={downloading || url == null}
            onClick={async () => {
              setDownloading((_) => true);
              await toast.promise(
                otherVisService.current!.downloadRepository(url!),
                {
                  loading: "Downloading...",
                  success: <b>Downloaded!</b>,
                  error: (err) => `Couldn't fetch: ${err}`,
                }
              );
              setDownloading((_) => false);
            }}
          >
            Download
          </Button>
        </div>
      </div>
    </>
  );
}
