export async function fetchProjectLogStats(projectId: number, token: string) {
  const response = await fetch(`/api/projects/${projectId}/log-stats`, {
    headers: {
      "Content-Type": "application/json",
      "Authorization": `Bearer ${token}`,
    },
  });
  if (!response.ok) throw new Error("API 요청 실패");
  return response.json();
}