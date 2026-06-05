'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { issuesApi } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
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
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuTrigger,
} from '@/components/ui/dropdown-menu';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { toast } from 'sonner';
import { Search, AlertTriangle, Loader2, Plus, MoreHorizontal } from 'lucide-react';
import type { Issue, PagedResponse, IssuePriority, IssueStatus } from '@/lib/types';

const priorities: IssuePriority[] = ['LOW', 'MEDIUM', 'HIGH', 'CRITICAL'];
const statuses: IssueStatus[] = ['REPORTED', 'ACKNOWLEDGED', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export default function IssuesPage() {
  const { isAdmin } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isResolveOpen, setIsResolveOpen] = useState(false);
  const [selectedIssue, setSelectedIssue] = useState<Issue | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const { data: allIssues, isLoading: allLoading, mutate: mutateAll } = useSWR<PagedResponse<Issue>>(
    isAdmin ? ['issues', page] : null,
    () => issuesApi.getAll(page, 10)
  );

  const { data: myIssues, isLoading: myLoading, mutate: mutateMy } = useSWR<Issue[]>(
    !isAdmin ? 'my-issues' : null,
    () => issuesApi.getMyIssues()
  );

  const isLoading = allLoading || myLoading;
  const issues = isAdmin ? allIssues?.content : myIssues;

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsProcessing(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await issuesApi.create({
        title: formData.get('title') as string,
        description: formData.get('description') as string,
        location: formData.get('location') as string,
        priority: formData.get('priority') as IssuePriority,
      });
      toast.success('Issue reported successfully');
      setIsCreateOpen(false);
      mutateAll();
      mutateMy();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to report issue');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleStatusChange = async (issueId: number, status: string, resolution?: string) => {
    try {
      await issuesApi.updateStatus(issueId, status, resolution);
      toast.success(`Status updated to ${status}`);
      mutateAll();
      mutateMy();
      setIsResolveOpen(false);
      setSelectedIssue(null);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update status');
    }
  };

  const handleResolve = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedIssue) return;
    setIsProcessing(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await handleStatusChange(
        selectedIssue.id,
        'RESOLVED',
        formData.get('resolution') as string
      );
    } finally {
      setIsProcessing(false);
    }
  };

  const filteredIssues = issues?.filter((issue) =>
    issue.title.toLowerCase().includes(search.toLowerCase()) ||
    issue.location?.toLowerCase().includes(search.toLowerCase()) ||
    issue.priority.toLowerCase().includes(search.toLowerCase())
  );

  const statusColors: Record<string, string> = {
    REPORTED: 'bg-warning/10 text-warning border-warning/20',
    ACKNOWLEDGED: 'bg-chart-2/10 text-chart-2 border-chart-2/20',
    IN_PROGRESS: 'bg-primary/10 text-primary border-primary/20',
    RESOLVED: 'bg-success/10 text-success border-success/20',
    CLOSED: 'bg-muted text-muted-foreground border-muted',
  };

  const priorityColors: Record<string, string> = {
    LOW: 'bg-muted text-muted-foreground border-muted',
    MEDIUM: 'bg-warning/10 text-warning border-warning/20',
    HIGH: 'bg-chart-4/10 text-chart-4 border-chart-4/20',
    CRITICAL: 'bg-destructive/10 text-destructive border-destructive/20',
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Issues</h1>
          <p className="text-muted-foreground">
            {isAdmin ? 'Manage maintenance issues' : 'Report and track maintenance issues'}
          </p>
        </div>
        <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              Report Issue
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>Report an Issue</DialogTitle>
              <DialogDescription>Report a maintenance issue in the society.</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleCreate} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="title">Title</Label>
                <Input id="title" name="title" placeholder="Brief description of the issue" required />
              </div>
              <div className="grid grid-cols-2 gap-4">
                <div className="space-y-2">
                  <Label htmlFor="location">Location</Label>
                  <Input id="location" name="location" placeholder="e.g., Lobby, Parking" required />
                </div>
                <div className="space-y-2">
                  <Label htmlFor="priority">Priority</Label>
                  <Select name="priority" required>
                    <SelectTrigger>
                      <SelectValue placeholder="Select priority" />
                    </SelectTrigger>
                    <SelectContent>
                      {priorities.map((priority) => (
                        <SelectItem key={priority} value={priority}>
                          {priority.charAt(0) + priority.slice(1).toLowerCase()}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  name="description"
                  placeholder="Provide detailed information about the issue"
                  rows={4}
                  required
                />
              </div>
              <div className="flex justify-end gap-2">
                <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
                  Cancel
                </Button>
                <Button type="submit" disabled={isProcessing}>
                  {isProcessing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                  Report Issue
                </Button>
              </div>
            </form>
          </DialogContent>
        </Dialog>
      </div>

      <Card className="bg-card border-border">
        <CardHeader>
          <div className="flex flex-col sm:flex-row sm:items-center gap-4">
            <div className="flex-1">
              <CardTitle className="text-foreground">
                {isAdmin ? 'All Issues' : 'My Issues'}
              </CardTitle>
              <CardDescription>
                {isAdmin
                  ? `${allIssues?.totalElements || 0} total issues`
                  : `${myIssues?.length || 0} issues reported`}
              </CardDescription>
            </div>
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search issues..."
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
          ) : filteredIssues && filteredIssues.length > 0 ? (
            <>
              <div className="rounded-lg border border-border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/50">
                      <TableHead>Title</TableHead>
                      <TableHead>Location</TableHead>
                      <TableHead>Priority</TableHead>
                      <TableHead>Status</TableHead>
                      <TableHead>Date</TableHead>
                      {isAdmin && <TableHead className="w-12"></TableHead>}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredIssues.map((issue) => (
                      <TableRow key={issue.id}>
                        <TableCell>
                          <div>
                            <p className="font-medium">{issue.title}</p>
                            <p className="text-sm text-muted-foreground line-clamp-1">
                              {issue.description}
                            </p>
                          </div>
                        </TableCell>
                        <TableCell>{issue.location}</TableCell>
                        <TableCell>
                          <Badge variant="outline" className={priorityColors[issue.priority] || ''}>
                            {issue.priority}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className={statusColors[issue.status] || ''}>
                            {issue.status.replace('_', ' ')}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {new Date(issue.createdAt).toLocaleDateString('en-IN')}
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
                                {statuses.map((status) => (
                                  <DropdownMenuItem
                                    key={status}
                                    onClick={() => {
                                      if (status === 'RESOLVED') {
                                        setSelectedIssue(issue);
                                        setIsResolveOpen(true);
                                      } else {
                                        handleStatusChange(issue.id, status);
                                      }
                                    }}
                                  >
                                    Mark as {status.replace('_', ' ')}
                                  </DropdownMenuItem>
                                ))}
                              </DropdownMenuContent>
                            </DropdownMenu>
                          </TableCell>
                        )}
                      </TableRow>
                    ))}
                  </TableBody>
                </Table>
              </div>

              {isAdmin && allIssues && allIssues.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {allIssues.totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={allIssues.first}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={allIssues.last}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center py-12">
              <AlertTriangle className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No issues found</p>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={isResolveOpen} onOpenChange={(open) => {
        setIsResolveOpen(open);
        if (!open) setSelectedIssue(null);
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Resolve Issue</DialogTitle>
            <DialogDescription>
              Provide resolution details for: {selectedIssue?.title}
            </DialogDescription>
          </DialogHeader>
          <form onSubmit={handleResolve} className="space-y-4">
            <div className="space-y-2">
              <Label htmlFor="resolution">Resolution</Label>
              <Textarea
                id="resolution"
                name="resolution"
                placeholder="Describe how the issue was resolved"
                rows={4}
                required
              />
            </div>
            <div className="flex justify-end gap-2">
              <Button type="button" variant="outline" onClick={() => setIsResolveOpen(false)}>
                Cancel
              </Button>
              <Button type="submit" disabled={isProcessing}>
                {isProcessing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                Mark Resolved
              </Button>
            </div>
          </form>
        </DialogContent>
      </Dialog>
    </div>
  );
}
