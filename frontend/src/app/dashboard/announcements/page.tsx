'use client';

import { useState } from 'react';
import useSWR from 'swr';
import { announcementsApi } from '@/lib/api';
import { useAuth } from '@/lib/auth-context';
import { Button } from '@/components/ui/button';
import { Input } from '@/components/ui/input';
import { Textarea } from '@/components/ui/textarea';
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from '@/components/ui/card';
import { Badge } from '@/components/ui/badge';
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from '@/components/ui/dialog';
import { Label } from '@/components/ui/label';
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select';
import { toast } from 'sonner';
import { Search, Megaphone, Loader2, Plus, Trash2 } from 'lucide-react';
import type { Announcement, PagedResponse, AnnouncementType } from '@/lib/types';

const announcementTypes: AnnouncementType[] = ['GENERAL', 'URGENT', 'MAINTENANCE', 'EVENT', 'MEETING'];

export default function AnnouncementsPage() {
  const { isAdmin } = useAuth();
  const [page, setPage] = useState(0);
  const [search, setSearch] = useState('');
  const [isCreateOpen, setIsCreateOpen] = useState(false);
  const [isProcessing, setIsProcessing] = useState(false);

  const { data, isLoading, mutate } = useSWR<PagedResponse<Announcement>>(
    ['announcements', page],
    () => announcementsApi.getAll(page, 10)
  );

  const handleCreate = async (e: React.FormEvent<HTMLFormElement>) => {
    e.preventDefault();
    setIsProcessing(true);
    const formData = new FormData(e.currentTarget);
    
    try {
      await announcementsApi.create({
        title: formData.get('title') as string,
        content: formData.get('content') as string,
        type: formData.get('type') as AnnouncementType,
        expiresAt: formData.get('expiresAt') as string || undefined,
      });
      toast.success('Announcement created successfully');
      setIsCreateOpen(false);
      mutate();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to create announcement');
    } finally {
      setIsProcessing(false);
    }
  };

  const handleDelete = async (id: number) => {
    if (!confirm('Are you sure you want to delete this announcement?')) return;
    
    try {
      await announcementsApi.delete(id);
      toast.success('Announcement deleted successfully');
      mutate();
    } catch (error) {
      toast.error(error instanceof Error ? error.message : 'Failed to delete announcement');
    }
  };

  const filteredAnnouncements = data?.content?.filter((announcement) =>
    announcement.title.toLowerCase().includes(search.toLowerCase()) ||
    announcement.content.toLowerCase().includes(search.toLowerCase()) ||
    announcement.type.toLowerCase().includes(search.toLowerCase())
  );

  const typeColors: Record<string, string> = {
    URGENT: 'bg-destructive/10 text-destructive border-destructive/20',
    GENERAL: 'bg-primary/10 text-primary border-primary/20',
    MAINTENANCE: 'bg-warning/10 text-warning border-warning/20',
    EVENT: 'bg-success/10 text-success border-success/20',
    MEETING: 'bg-chart-5/10 text-chart-5 border-chart-5/20',
  };

  const typeIcons: Record<string, string> = {
    URGENT: 'border-l-destructive',
    GENERAL: 'border-l-primary',
    MAINTENANCE: 'border-l-warning',
    EVENT: 'border-l-success',
    MEETING: 'border-l-chart-5',
  };

  return (
    <div className="space-y-6">
      <div className="flex flex-col sm:flex-row sm:items-center sm:justify-between gap-4">
        <div>
          <h1 className="text-2xl font-bold text-foreground">Announcements</h1>
          <p className="text-muted-foreground">
            {isAdmin ? 'Create and manage society announcements' : 'View society announcements'}
          </p>
        </div>
        {isAdmin && (
          <Dialog open={isCreateOpen} onOpenChange={setIsCreateOpen}>
            <DialogTrigger asChild>
              <Button>
                <Plus className="mr-2 h-4 w-4" />
                New Announcement
              </Button>
            </DialogTrigger>
            <DialogContent className="max-w-lg">
              <DialogHeader>
                <DialogTitle>Create Announcement</DialogTitle>
                <DialogDescription>Post an announcement to all society members.</DialogDescription>
              </DialogHeader>
              <form onSubmit={handleCreate} className="space-y-4">
                <div className="space-y-2">
                  <Label htmlFor="title">Title</Label>
                  <Input id="title" name="title" placeholder="Announcement title" required />
                </div>
                <div className="grid grid-cols-2 gap-4">
                  <div className="space-y-2">
                    <Label htmlFor="type">Type</Label>
                    <Select name="type" required>
                      <SelectTrigger>
                        <SelectValue placeholder="Select type" />
                      </SelectTrigger>
                      <SelectContent>
                        {announcementTypes.map((type) => (
                          <SelectItem key={type} value={type}>
                            {type.charAt(0) + type.slice(1).toLowerCase()}
                          </SelectItem>
                        ))}
                      </SelectContent>
                    </Select>
                  </div>
                  <div className="space-y-2">
                    <Label htmlFor="expiresAt">Expires On (Optional)</Label>
                    <Input id="expiresAt" name="expiresAt" type="date" />
                  </div>
                </div>
                <div className="space-y-2">
                  <Label htmlFor="content">Content</Label>
                  <Textarea
                    id="content"
                    name="content"
                    placeholder="Write your announcement here..."
                    rows={5}
                    required
                  />
                </div>
                <div className="flex justify-end gap-2">
                  <Button type="button" variant="outline" onClick={() => setIsCreateOpen(false)}>
                    Cancel
                  </Button>
                  <Button type="submit" disabled={isProcessing}>
                    {isProcessing ? <Loader2 className="mr-2 h-4 w-4 animate-spin" /> : null}
                    Publish
                  </Button>
                </div>
              </form>
            </DialogContent>
          </Dialog>
        )}
      </div>

      <div className="flex items-center gap-4">
        <div className="relative flex-1 max-w-md">
          <Search className="absolute left-3 top-1/2 -translate-y-1/2 h-4 w-4 text-muted-foreground" />
          <Input
            placeholder="Search announcements..."
            value={search}
            onChange={(e) => setSearch(e.target.value)}
            className="pl-9 bg-input"
          />
        </div>
      </div>

      {isLoading ? (
        <div className="flex items-center justify-center py-12">
          <Loader2 className="h-8 w-8 animate-spin text-primary" />
        </div>
      ) : filteredAnnouncements && filteredAnnouncements.length > 0 ? (
        <div className="space-y-4">
          {filteredAnnouncements.map((announcement) => (
            <Card
              key={announcement.id}
              className={`bg-card border-border border-l-4 ${typeIcons[announcement.type] || 'border-l-primary'}`}
            >
              <CardHeader className="pb-2">
                <div className="flex items-start justify-between gap-4">
                  <div className="flex-1">
                    <div className="flex items-center gap-2 mb-1">
                      <Badge variant="outline" className={typeColors[announcement.type] || ''}>
                        {announcement.type}
                      </Badge>
                      <span className="text-xs text-muted-foreground">
                        {new Date(announcement.createdAt).toLocaleDateString('en-IN', {
                          year: 'numeric',
                          month: 'short',
                          day: 'numeric',
                        })}
                      </span>
                    </div>
                    <CardTitle className="text-foreground text-lg">{announcement.title}</CardTitle>
                  </div>
                  {isAdmin && (
                    <Button
                      variant="ghost"
                      size="icon"
                      className="text-destructive hover:text-destructive hover:bg-destructive/10"
                      onClick={() => handleDelete(announcement.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  )}
                </div>
              </CardHeader>
              <CardContent>
                <p className="text-muted-foreground whitespace-pre-wrap">{announcement.content}</p>
                <div className="flex items-center gap-4 mt-4 pt-4 border-t border-border text-sm text-muted-foreground">
                  <span>Posted by {announcement.createdBy}</span>
                  {announcement.expiresAt && (
                    <span>
                      Expires: {new Date(announcement.expiresAt).toLocaleDateString('en-IN')}
                    </span>
                  )}
                </div>
              </CardContent>
            </Card>
          ))}

          {data && data.totalPages > 1 && (
            <div className="flex items-center justify-between pt-4">
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
        </div>
      ) : (
        <Card className="bg-card border-border">
          <CardContent className="flex flex-col items-center justify-center py-12">
            <Megaphone className="h-12 w-12 text-muted-foreground mb-4" />
            <p className="text-muted-foreground">No announcements found</p>
          </CardContent>
        </Card>
      )}
    </div>
  );
}
