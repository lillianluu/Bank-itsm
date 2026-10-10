import http from './http';

export interface DeptNode {
  id: number;
  parentId: number;
  name: string;
  sort: number;
  leader: string | null;
  phone: string | null;
  email: string | null;
  status: 0 | 1;
  children: DeptNode[];
}
export type DeptSaveCmd = Omit<DeptNode, 'id' | 'children'>;
export function fetchDeptTree(): Promise<DeptNode[]> {
  return http.get<never, DeptNode[]>('/depts/tree');
}
export function createDept(cmd: DeptSaveCmd): Promise<number> {
  return http.post<never, number>('/depts', cmd);
}
export function updateDept(id: number, cmd: DeptSaveCmd): Promise<void> {
  return http.put<never, void>(`/depts/${id}`, cmd);
}
export function deleteDept(id: number): Promise<void> {
  return http.delete<never, void>(`/depts/${id}`);
}
