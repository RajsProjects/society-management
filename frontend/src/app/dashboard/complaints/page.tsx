'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { complaintsApi } from '@/lib/api';
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
import { Search, MessageSquareWarning, Loader2, Plus, MoreHorizontal } from 'lucide-react';
import type { Complaint, PagedResponse, ComplaintCategory, ComplaintStatus } from '@/lib/types';

const categories: ComplaintCategory[] = ['MAINTENANCE', 'SECURITY', 'NOISE', 'PARKING', 'CLEANLINESS', 'OTHER'];
const statuses: ComplaintStatus[] = ['OPEN', 'IN_PROGRESS', 'RESOLVED', 'CLOSED'];

export default function ComplaintsPage() {
  const { isAdmin, user } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isResolveOpen, setIsResolveOpen] = useState(false);
  const [selectedComplaint, setSelectedComplaint] = useState<Complaint | null>(null);
  const [isProcessing, setIsProcessing] = useState(false);

  const { data: allComplaints, isLoading: allLoading, mutate: mutateAll } = useSWR<PagedResponse<Complaint>>(
    isAdmin ? ['complaints', page] : null,
    () => complaintsApi.getAll(page, 10)
  );

  const { data: myComplaints, isLoading: myLoading, mutate: mutateMy } = useSWR<Complaint[]>(
    !isAdmin ? 'my-complaints' : null,
    () => complaintsApi.getMyComplaints()
  );

  const isLoading = allLoading || myLoading;
  const complaints = isAdmin ? allComplaints?.content : myComplaints;

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsProcessing(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await complaintsApi.create({
        title: formData.get('title') as string,
        description: formData.get('description') as string,
        category: formData.get('category') as ComplaintCategory,
        flatId: user?.flatId || 0,
      });
      toast.success('Complaint submitted successfully');
      setIsCreateOpen(false);
      mutateAll();
      mutateMy();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to submit complaint');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleStatusChange = async (complaintId: number, status: string, resolution?: string) => {
    try {
      await complaintsApi.updateStatus(complaintId, status, resolution);
      toast.success(`Status updated to ${status}`);
      mutateAll();
      mutateMy();
      setIsResolveOpen(false);
      setSelectedComplaint(null);
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to update status');
    }
  };

  const handleResolve = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    if (!selectedComplaint) return;
    setIsProcessing(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await handleStatusChange(
        selectedComplaint.id,
        'RESOLVED',
        formData.get('resolution') as string
      );
    } finally {
      setIsProcessing(false);
    }
  };

  const filteredComplaints = complaints?.filter((complaint) =>
    complaint.title.toLowerCase().includes(search.toLowerCase()) ||
    complaint.category.toLowerCase().includes(search.toLowerCase()) ||
    complaint.flatNumber?.toLowerCase().includes(search.toLowerCase())
  );

  const statusColors: Record<string, string> = {
    OPEN: 'bg-warning/10 text-warning border-warning/20',
    IN_PROGRESS: 'bg-chart-2/10 text-chart-2 border-chart-2/20',
    RESOLVED: 'bg-success/10 text-success border-success/20',
    CLOSED: 'bg-muted text-muted-foreground border-muted',
  };

  const categoryColors: Record<string, string> = {
    MAINTENANCE: 'bg-primary/10 text-primary border-primary/20',
    SECURITY: 'bg-destructive/10 text-destructive border-destructive/20',
    NOISE: 'bg-warning/10 text-warning border-warning/20',
    PARKING: 'bg-chart-2/10 text-chart-2 border-chart-2/20',
    CLEANLINESS: 'bg-success/10 text-success border-success/20',
    OTHER: 'bg-muted text-muted-foreground border-muted',
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Complaints</h1>
          <p className="text-muted-foreground">
            {isAdmin ? 'Manage resident complaints' : 'File and track your complaints'}
          </p>
        </div>
        <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
          <DialogTrigger asChild>
            <Button>
              <Plus className="mr-2 h-4 w-4" />
              New Complaint
            </Button>
          </DialogTrigger>
          <DialogContent>
            <DialogHeader>
              <DialogTitle>File a Complaint</DialogTitle>
              <DialogDescription>Describe your issue and we will address it promptly.</DialogDescription>
            </DialogHeader>
            <form onSubmit={handleCreate} className="space-y-4">
              <div className="space-y-2">
                <Label htmlFor="title">Title</Label>
                <Input id="title" name="title" placeholder="Brief description of the issue" required />
              </div>
              <div className="space-y-2">
                <Label htmlFor="category">Category</Label>
                <Select name="category" required>
                  <SelectTrigger>
                    <SelectValue placeholder="Select category" />
                  </SelectTrigger>
                  <SelectContent>
                    {categories.map((cat) => (
                      <SelectItem key={cat} value={cat}>
                        {cat.charAt(0) + cat.slice(1).toLowerCase()}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-2">
                <Label htmlFor="description">Description</Label>
                <Textarea
                  id="description"
                  name="description"
                  placeholder="Provide detailed information about the complaint"
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
                  Submit Complaint
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
                {isAdmin ? 'All Complaints' : 'My Complaints'}
              </CardTitle>
              <CardDescription>
                {isAdmin
                  ? `${allComplaints?.totalElements || 0} total complaints`
                  : `${myComplaints?.length || 0} complaints filed`}
              </CardDescription>
            </div>
            <div className="relative w-full sm:w-64">
              <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
              <Input
                placeholder="Search complaints..."
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
          ) : filteredComplaints && filteredComplaints.length > 0 ? (
            <>
              <div className="rounded-lg border border-border overflow-hidden">
                <Table>
                  <TableHeader>
                    <TableRow className="bg-muted/50">
                      <TableHead>Title</TableHead>
                      <TableHead>Category</TableHead>
                      {isAdmin && <TableHead>Flat</TableHead>}
                      <TableHead>Status</TableHead>
                      <TableHead>Date</TableHead>
                      {isAdmin && <TableHead className="w-12"></TableHead>}
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {filteredComplaints.map((complaint) => (
                      <TableRow key={complaint.id}>
                        <TableCell>
                          <div>
                            <p className="font-medium">{complaint.title}</p>
                            <p className="text-sm text-muted-foreground line-clamp-1">
                              {complaint.description}
                            </p>
                          </div>
                        </TableCell>
                        <TableCell>
                          <Badge variant="outline" className={categoryColors[complaint.category] || ''}>
                            {complaint.category}
                          </Badge>
                        </TableCell>
                        {isAdmin && <TableCell>{complaint.flatNumber}</TableCell>}
                        <TableCell>
                          <Badge variant="outline" className={statusColors[complaint.status] || ''}>
                            {complaint.status.replace('_', ' ')}
                          </Badge>
                        </TableCell>
                        <TableCell>
                          {new Date(complaint.createdAt).toLocaleDateString('en-IN')}
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
                                        setSelectedComplaint(complaint);
                                        setIsResolveOpen(true);
                                      } else {
                                        handleStatusChange(complaint.id, status);
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

              {isAdmin && allComplaints && allComplaints.totalPages > 1 && (
                <div className="flex items-center justify-between mt-4">
                  <p className="text-sm text-muted-foreground">
                    Page {page + 1} of {allComplaints.totalPages}
                  </p>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => Math.max(0, p - 1))}
                      disabled={allComplaints.first}
                    >
                      Previous
                    </Button>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => setPage((p) => p + 1)}
                      disabled={allComplaints.last}
                    >
                      Next
                    </Button>
                  </div>
                </div>
              )}
            </>
          ) : (
            <div className="flex flex-col items-center justify-center py-12">
              <MessageSquareWarning className="h-12 w-12 text-muted-foreground mb-4" />
              <p className="text-muted-foreground">No complaints found</p>
            </div>
          )}
        </CardContent>
      </Card>

      <Dialog open={isResolveOpen} onOpenChange={(open) => {
        setIsResolveOpen(open);
        if (!open) setSelectedComplaint(null);
      }}>
        <DialogContent>
          <DialogHeader>
            <DialogTitle>Resolve Complaint</DialogTitle>
            <DialogDescription>
              Provide resolution details for: {selectedComplaint?.title}
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
