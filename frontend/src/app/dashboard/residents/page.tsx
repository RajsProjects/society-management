'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { usersApi } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { toast } from 'sonner';
import { Search, Users, Loader2, MoreHorizontal, CheckCircle, XCircle } from 'lucide-react';
import type { UserSummary, PagedResponse } from '@/lib/types';

export default function ResidentsPage() {
  const { isAdmin } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');

  const { data, isLoading, mutate } = useSWR<PagedResponse<UserSummary>>(
    ['residents', page],
    () => usersApi.getAll(page, 10)
  );

  const { data: pendingUsers, mutate: mutatePending } = useSWR<UserSummary[]>(
    isAdmin ? 'pending-users' : null,
    () => usersApi.getPendingApprovals()
  );

  const handleStatusChange = async (userId: number, status: string) => {
    try {
      await usersApi.updateStatus(userId, status);
      toast.success(`User status updated to ${status}`);
      mutate();
      mutatePending();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update status');
    }
  };

  const handleApprove = async (userId: number) => {
    try {
      await usersApi.approveUser(userId);
      toast.success('User approved successfully');
      mutate();
      mutatePending();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to approve user');
    }
  };

  const filteredResidents = data?.content?.filter((user) =>
    user.name.toLowerCase().includes(search.toLowerCase()) ||
    user.email.toLowerCase().includes(search.toLowerCase()) ||
    user.flatNumber?.toLowerCase().includes(search.toLowerCase())
  );

  const statusColors: Record<string, string> = {
    ACTIVE: 'bg-success/10 text-success border-success/20',
    INACTIVE: 'bg-muted text-muted-foreground border-muted',
    PENDING: 'bg-warning/10 text-warning border-warning/20',
  };

  const roleColors: Record<string, string> = {
    SUPER_ADMIN: 'bg-destructive/10 text-destructive border-destructive/20',
    ADMIN: 'bg-primary/10 text-primary border-primary/20',
    ACCOUNTANT: 'bg-chart-2/10 text-chart-2 border-chart-2/20',
    SECURITY: 'bg-warning/10 text-warning border-warning/20',
    RESIDENT: 'bg-muted text-muted-foreground border-muted',
  };

  return (
    <div className="space-y-6">
      <div>
        <h1 className="text-2xl font-bold text-foreground">Residents</h1>
        <p className="text-muted-foreground">Manage society members and residents</p>
      </div>

      {isAdmin && pendingUsers && pendingUsers.length > 0 && (
        <Card className="bg-card border-warning/30">
          <CardHeader>
            <CardTitle className="text-foreground flex items-center gap-2">
              <span className="flex h-2 w-2 rounded-full bg-warning animate-pulse"></span>
              Pending Approvals
            </CardTitle>
            <CardDescription>
              {pendingUsers.length} user(s) waiting for approval
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="space-y-3">
              {pendingUsers.map((user) => (
                <div
                  key={user.id}
                  className="flex items-center justify-between p-3 rounded-lg border border-border bg-card/50"
                >
                  <div>
                    <p className="font-medium text-foreground">{user.name}</p>
                    <p className="text-sm text-muted-foreground">{user.email}</p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      size="sm"
                      variant="outline"
                      className="text-success border-success/20 hover:bg-success/10"
                      onClick={() => handleApprove(user.id)}
                    >
                      <CheckCircle className="h-4 w-4 mr-1" />
                      Approve
                    </Button>
                    <Button
                      size="sm"
                      variant="outline"
                      className="text-destructive border-destructive/20 hover:bg-destructive/10"
                      onClick={() => handleStatusChange(user.id, 'INACTIVE')}
                    >
                      <XCircle className="h-4 w-4 mr-1" />
                      Reject
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          </CardContent>
        </Card>
      )}

      <Card className="bg-card border-border">
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center gap-4">
            <div className="flex-1">
              <CardTitle className="text-foreground">All Members</CardTitle>
              <CardDescription>
                {data?.totalElements || 0} registered members
              </CardDescription>
            </div>
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search residents..."
                value={search}
                onChange={(e) => setSearch(e.target.value)}
                className="pl-9 bg-input"
              />
            </div>
          </div>
        </CardHeader>
        <CardContent>
          {isLoading ? (
            <div className="flex items-center justify-center py-12">
              <Loader2 className="h-8 w-8 animate-spin text-primary" />
            </div>
          ) : filteredResidents && filteredResidents.length > 0 ? (
            <>
              <div className="rounded-lg border border-border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/50">
                      <TableHead>Name</TableHead>
                      <TableHead>Email</TableHead>
                      <TableHead>Phone</TableHead>
                      <TableHead>Flat</TableHead>
                      <TableHead>Role</TableHead>
                      <TableHead>Status</TableHead>
                      {isAdmin && <TableHead className="w-12"></TableHead>}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredResidents.map((user) => (
                      <TableRow key={user.id}>
                        <TableCell className="font-medium">{user.name}</TableCell>
                        <TableCell>{user.email}</TableCell>
                        <TableCell>{user.phone}</TableCell>
                        <TableCell>
                          {user.flatNumber || <span className="text-muted-foreground">-</span>}
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className={roleColors[user.role] || ''}>
                            {user.role}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className={statusColors[user.status] || ''}>
                            {user.status}
                          </Badge>
                        </TableCell>
                        {isAdmin && (
                          <TableCell>
                            <DropdownMenu>
                              <DropdownMenuTrigger asChild>
                                <Button variant="ghost" size="icon">
                                  <MoreHorizontal className="h-4 w-4" />
                                </Button>
                              </DropdownMenuTrigger>
                              <DropdownMenuContent align="end">
                                <DropdownMenuItem
                                  onClick={() => handleStatusChange(user.id, 'ACTIVE')}
                                >
                                  Set Active
                                </DropdownMenuItem>
                                <DropdownMenuItem
                                  onClick={() => handleStatusChange(user.id, 'INACTIVE')}
                                >
                                  Set Inactive
                                </DropdownMenuItem>
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {data && data.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {data.totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={data.first}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={data.last}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center py-12">
              <Users className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No residents found</p>
            </div>
          )}
        </CardContent>
      </Card>
    </div>
  );
}
